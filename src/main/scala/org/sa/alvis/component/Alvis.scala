package org.sa.alvis.component

import java.io.File

import jline.console.ConsoleReader
import jline.console.history.FileHistory
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.sa.alvis.common.{AlvisConfigConstants, AlvisConstants}
import org.sa.alvis.completer.AlvisCompleter
import org.sa.alvis.connection.DatabaseConnections
import org.sa.alvis.format.Formatter
import org.sa.alvis.functions._
import org.sa.utils.database.common.Drivers
import org.sa.utils.hadoop.constants.{HDFSConfigConstants, YarnConfigConstants, ZookeeperConfigConstants}
import org.sa.utils.hadoop.hbase.HBaseHandler
import org.sa.utils.hadoop.hdfs.{FileSystemHandler, HDFSHandler, HighAvailableHDFSHandler, LocalFileSystemHandler}
import org.sa.utils.spark.SparkUtils
import org.sa.utils.spark.hbase.SparkHBaseHandler
import org.sa.utils.universal.base.Joker
import org.sa.utils.universal.cli.PrintConfig
import org.sa.utils.universal.config.Config
import org.sa.utils.universal.implicits.BasicConversions._
import org.sa.utils.universal.sql.ScriptAnalyser

import scala.collection.JavaConversions._
import scala.io.StdIn
import scala.util.Try

/**
 * Created by Stuart Alex on 2017/9/7.
 */
case class Alvis(implicit val config: Config) extends AlvisConfigConstants with HDFSConfigConstants with YarnConfigConstants with ZookeeperConfigConstants with PrintConfig {
    lazy val alvisCompleter = new AlvisCompleter(config)
    lazy val commonCommands: CommonCommands = CommonCommands(this)
    lazy val comparator: Comparator = Comparator(this)
    lazy val exporter: Exporter = Exporter(this)
    lazy val formatter: Formatter = Formatter(this)
    lazy val hbaseCommands: HBaseCommands = HBaseCommands(this)
    lazy val informer: Informer = Informer(this)
    lazy val logViewer: LogViewer = LogViewer(this)
    lazy val query: Query = Query(this)
    lazy val tableModifier: TableModifier = TableModifier(this)
    lazy val sparkHBaseHandler: SparkHBaseHandler = SparkHBaseHandler(ZOOKEEPER_QUORUM.stringValue, ZOOKEEPER_PORT.intValue)
    lazy val hbaseHandler: HBaseHandler = HBaseHandler(ZOOKEEPER_QUORUM.stringValue, ZOOKEEPER_PORT.intValue)
    lazy val fsHandler: FileSystemHandler = if (HDFS_ENABLED.booleanValue) {
        if (HDFS_HA.booleanValue)
            new HighAvailableHDFSHandler(HDFS_NAMESERVICE.stringValue, HDFS_NAMENODES.arrayValue(), HDFS_PORT.intValue)
        else
            new HDFSHandler(HDFS_NAMENODE_ADDRESS.stringValue)
    } else {
        LocalFileSystemHandler
    }
    lazy val sparkSession: SparkSession = SparkUtils.getSparkSession()
    lazy val sparkContext: SparkContext = sparkSession.sparkContext
    private lazy val os = System.getProperty("os.name").toLowerCase
    private val consoleReader = new ConsoleReader()
    private val historyFile = new File(HISTORY_FILE_PATH)
    private val fileHistory = new FileHistory(historyFile)
    private val welcome =
        """Welcome to
          |        _     _         _
          |       / \   | |__   __(_) ___
          |      / _ \  | |\ \ / /| |/ __|
          |     / ___ \ | | \ V / | |\__ \    version 1.0
          |    /_/   \_\|_|  \_/  |_||___/""".stripMargin

    def start(): Unit = {
        Try(Drivers.Hive.load())
        Try(Drivers.MySQL.load())
        Try(Drivers.PRESTO.load())
        this.initializeConsoleReader()
        this.welcome.prettyPrintln(PRINT_RENDER.stringValue)
        this.initializeConnection()
        while (!CONSOLE_EXIT.booleanValue) {
            val isSilent = Try(CONSOLE_SILENT.booleanValue)
            val line = if (isSilent.isSuccess && isSilent.get)
                this.readLine(this.getPrompt, ConsoleReader.NULL_MASK)
            else
                this.readLine(this.getPrompt)
            if (line.notNull)
                try {
                    this.dispatch(line.trim)
                }
                catch {
                    case t: Throwable => ExceptionHandler.handle(t)
                }
        }
    }

    def initializeConnection(): Unit = {
        if (CONNECTION_ON_START.isDefined) {
            val onStart = CONNECTION_ON_START.stringValue
            commonCommands.connect(s"connect $onStart")
        }
    }

    def initializeConsoleReader(): Unit = {
        this.consoleReader.addCompleter(alvisCompleter)
        this.consoleReader.setExpandEvents(false)
        if (!historyFile.exists() || !historyFile.isFile) {
            historyFile.getParentFile.mkdirs()
            historyFile.createNewFile()
        }
        this.consoleReader.setHistoryEnabled(false)
        this.consoleReader.setHistory(this.fileHistory)
        Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
            override def run(): Unit = {
                commonCommands.closeAll(null)
                flushHistory()
                fileHistory.flush()
            }
        }))
    }

    /**
     * 重写command history，把command里面的换行符换成空格
     */
    def flushHistory(): Unit = {
        val history = this.fileHistory.map(_.value().toString)
        this.fileHistory.clear()
        ScriptAnalyser.squeeze(history.map(_.split("\n").mkString(" ")).toArray)
            .foreach(this.fileHistory.add)
    }

    def dispatch(line: String): Unit = {
        if (line.notNullAndEmpty && !line.isComment) {
            if (line == "?" || line == "help")
                commonCommands.handleCommand(s"help")
            else if (line == ":)")
                Joker.one().prettyPrintln(PRINT_RENDER.stringValue)
            else if (line.startsWith(AlvisConstants.COMMAND_PREFIX)) {
                this.addHistory(line)
                commonCommands.handleCommand(line.substring(AlvisConstants.COMMAND_PREFIX.length))
            } else
                commonCommands.handleQuery(line)
        }
    }

    def addHistory(line: String): Unit = {
        //    if (this.fileHistory.map(_.value().toString).contains(line)) {
        //      //去掉历史记录里相同的命令
        //      while (this.fileHistory.exists(_.value() == line))
        //        this.fileHistory.remove(this.fileHistory.find(_.value() == line).get.index())
        //    }
        this.fileHistory.add(line)
    }

    def getPrompt: String = {
        if (DatabaseConnections.current.isNull || DatabaseConnections.current.url.isNull) {
            if (DatabaseConnections.index == -1)
                s"${CliDescriptor.programName}(hive${DatabaseConnections.hiveDatabase})>  "
            else if (DatabaseConnections.index == -2)
                s"${CliDescriptor.programName}(hbase)>  "
            else
                s"${CliDescriptor.programName}>  "
        }
        else {
            val url = DatabaseConnections.current.url
            DatabaseConnections.index + " " + url + DatabaseConnections.current.currentDatabase + s"${if (DatabaseConnections.current.isClosed) "(closed)" else ""}>  "
        }
    }

    def readLine(prompt: String, mask: Character = null): String = {
        if (os.contains("windows")) {
            StdIn.readLine(prompt.rendering(PRINT_RENDER.stringValue))
        }
        else {
            if (mask.notNull)
                this.consoleReader.readLine(prompt.rendering(PRINT_RENDER.stringValue), mask)
            else
                this.consoleReader.readLine(prompt.rendering(PRINT_RENDER.stringValue))
        }
    }

    def getConsoleReader: ConsoleReader = this.consoleReader

}

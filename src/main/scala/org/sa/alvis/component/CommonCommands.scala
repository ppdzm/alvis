package org.sa.alvis.component

import java.util.Properties

import org.sa.alvis.common.{AlvisConstants, Messages}
import org.sa.alvis.connection.DatabaseConnections
import org.sa.alvis.options.major.ScriptOption
import org.sa.utils.hadoop.hbase.HBaseCatalog
import org.sa.utils.spark.sql.SparkSQL
import org.sa.utils.universal.base.Symbols._
import org.sa.utils.universal.cli.{CliUtils, ParameterOption}
import org.sa.utils.universal.implicits.BasicConversions._
import org.sa.utils.universal.implicits.ConfiguredConversions
import org.sa.utils.universal.sql.ScriptAnalyser

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Try

/**
 * Created by Stuart Alex on 2017/9/7.
 */
case class CommonCommands(alvis: Alvis) {

    import alvis._

    private lazy val supportedProtocols = Array("hive2", "mysql", "presto")
    @transient protected var lastException: Throwable = _
    private val configuredConversions = new ConfiguredConversions()

    def handleCommand(line: String): Unit = {
        val command = line.splitDoubleQuotedString(" ").filter(_.nonEmpty).head
        val commandsMap = mutable.Map[String, CommandHandler]()
        CommandHandler.handlers(config).foreach(handler => {
            val matches = handler.matches(command)
            if (matches.notNull)
                commandsMap += matches -> handler
        })
        commandsMap.size match {
            case 0 => Messages.`unknown-command`(command).prettyPrintln(PRINT_RENDER.stringValue)
            case 1 => executeCommand(commandsMap.valuesIterator.next(), line)
            case _ =>
                if (commandsMap.get(command).isDefined)
                    executeCommand(commandsMap(command), line)
                else
                    Messages.`multiple-matches`(commandsMap.keys.mkString(", ")).prettyPrintln(PRINT_RENDER.stringValue)
        }
    }

    def executeCommand(commandHandler: CommandHandler, line: String): Unit = {
        this.lastException = null
        val command = commandHandler.commands.head
        val method = Try(getClass.getMethod(command, classOf[String]))
        if (method.isFailure && method.failed.get.isInstanceOf[NoSuchMethodException]) {
            Messages.`no-such-method`(command, getClass.getName).prettyPrintln(PRINT_RENDER.stringValue)
        } else {
            //method.get.invoke(Commands, line)
            val r = Try(method.get.invoke(this, line))
            if (r.isFailure) {
                this.lastException = r.failed.get
                ExceptionHandler.handle(this.lastException)
            }
        }
    }

    def alter(line: String): Unit = {
        val args = line.split(" ").map(_.trim).filter(_.nonEmpty).drop(1)
        val result = Try(tableModifier.alter(CliDescriptor.createCli(config, args)))
        if (result.isFailure)
            (result.failed.get.getMessage + lineSeparator + Messages.`usage-alter`).prettyPrintln(PRINT_RENDER.stringValue)
    }

    def columns(line: String): Unit = {
        val parts = line.splitDoubleQuotedString(" ").map(_.trim).filter(_.nonEmpty)
        if (parts.length != 2)
            Messages.`usage-columns`.prettyPrintln(PRINT_RENDER.stringValue)
        else {
            if (DatabaseConnections.index != -2)
                this.handleQuery(s"desc ${parts(1)};")
        }
    }

    def compare(line: String): Unit = {
        val args = line.split(" ").map(_.trim).filter(_.nonEmpty).drop(1)
        val result = Try(comparator.compare(CliDescriptor.createCli(config, args)))
        if (result.isFailure)
            (result.failed.get.getMessage + lineSeparator + Messages.`usage-compare`).prettyPrintln(PRINT_RENDER.stringValue)
    }

    def connect(line: String): Unit = {
        val args = line.split(" ").map(_.trim).filter(_.nonEmpty)
        if (args.length < 2) {
            Messages.`usage-connect`.prettyPrintln(PRINT_RENDER.stringValue)
            return
        }
        val url = args(1)
        if (url.startsWith(AlvisConstants.REFERENCE_PREFIX)) {
            val name = url.trimStart(AlvisConstants.REFERENCE_PREFIX)
            connect(s"connect ${config.newConfigItem(s"connection.$name.url").stringValue}")
            return
        }
        val segsOfUrl = url.split(":")
        val driverProvided = if (segsOfUrl.length > 2) {
            val protocol = segsOfUrl(1)
            if (!this.supportedProtocols.contains(protocol)) {
                if (args.length == 5) {
                    //use command line provided driver
                    Class.forName(args(4))
                    true
                } else {
                    val driver = readLine(s"Enter driver name for $url : ")
                    val tryForName = Try(Class.forName(driver))
                    if (tryForName.isFailure)
                        ExceptionHandler.handle(tryForName.failed.get)
                    tryForName.isSuccess
                }
            } else {
                true
            }
        } else {
            Messages.`usage-connect`.prettyPrintln(PRINT_RENDER.stringValue)
            false
        }
        if (driverProvided) {
            val parameters = if (!url.contains("?"))
                Map[String, String]()
            else
                url.substring(url.indexOf("?") + 1).trim.split("&").map(_.trim).map(_.split("=").map(_.trim))
                    .map(e => e(0) -> {
                        if (e.length > 1) e(1) else ""
                    }).toMap
            val recordedUrl = if (!url.contains("?")) url else url.substring(0, url.indexOf("?"))
            val user = parameters.getOrElse("username", parameters.getOrElse("user", readLine(s"Enter username for $recordedUrl : ")))
            val password = parameters.getOrElse("password", readLine(s"Enter password for $recordedUrl : ", '*'))
            Messages.`connecting`(recordedUrl).prettyPrintln(PRINT_RENDER.stringValue)
            val properties = new Properties()
            parameters.foreach(e => properties.put(e._1, e._2))
            if (user != null)
                properties.put("user", user)
            if (password != null)
                properties.put("password", password)
            DatabaseConnections.create(recordedUrl, properties)
        }
    }

    def execute(line: String): Unit = {
        val args = line.split(" ").map(_.trim).filter(_.nonEmpty).drop(1)
        val cli = CliDescriptor.createCli(config, args)
        if (!cli.hasOption(ScriptOption.name)) {
            Messages.`usage-execute`.prettyPrintln(PRINT_RENDER.stringValue)
        } else {
            val result = Try(ScriptExecutor(cli, alvis, interactive = true).execute())
            if (result.isFailure)
                ExceptionHandler.handle(result.failed.get)
        }
    }

    def get(line: String): Unit = {
        val parts = line.split(" ").map(_.trim).filter(_.nonEmpty)
        if (parts.length != 2)
            Messages.`usage-!get`.prettyPrintln(PRINT_RENDER.stringValue)
        else {
            val value = Try(config.newConfigItem(parts(1)).stringValue)
            if (value.isSuccess)
                (s"value of ${parts(1)} is " + value.get).prettyPrintln(PRINT_RENDER.stringValue)
            else
                s"configuration ${parts(1)} is not defined yet".prettyPrintln(PRINT_RENDER.stringValue)
        }
    }

    def generate(line: String): Unit = {
        val cli = CliDescriptor.createCli(config, line.split(" ").map(_.trim).filter(_.nonEmpty).drop(1))
        val result = Try(HBaseCatalog(cli.getOptionProperties(ParameterOption.name)).display(PRINT_RENDER.stringValue))
        if (result.isFailure)
            result.failed.get.getMessage.prettyPrintln(PRINT_RENDER.stringValue)
    }

    def go(line: String): Unit = {
        val parts = line.split(" ").map(_.trim).filter(_.nonEmpty)
        if (parts.isNull || parts.isEmpty || parts.length < 2) {
            Messages.`usage-go`.prettyPrintln(PRINT_RENDER.stringValue)
        } else {
            val index = parts(1)
            if (Try(index.toInt).isFailure)
                Messages.`invalid-connection`(index).prettyPrintln(PRINT_RENDER.stringValue)
            else {
                if (!DatabaseConnections.setIndex(index.toInt)) {
                    Messages.`invalid-connection`(index).prettyPrintln(PRINT_RENDER.stringValue)
                    this.list(null)
                }
            }
        }
    }

    def list(line: String): Unit = {
        import configuredConversions._
        DatabaseConnections
            .getConnections
            .zipWithIndex.map(e => List(e._2, e._1.status, e._1.url + e._1.currentDatabase))
            .toList
            .+:(List(-1, "open", "Hive"))
            .+:(List(-2, "open", "HBase"))
            .prettyShow(columns = List(Messages.`column-order-number`, Messages.`column-status`, Messages.`column-address`))
    }

    def help(line: String): Unit = {
        val help =
            if (DatabaseConnections.index == -2) {
                alvisCompleter.supportedHBaseCommands.map(m => (m, Messages.`usage`(m))).toList
            } else {
                CommandHandler.handlers(config).map(e => (e.commands.map(AlvisConstants.COMMAND_PREFIX + _).mkString(", "), e.help))
            }
        CliUtils.printHelp(help, PRINT_RENDER.stringValue)
    }

    def history(line: String): Unit = {
        val historyBuffer = ListBuffer[String]()
        getConsoleReader.getHistory.entries().foreach(e => {
            val value = e.value().toString
            if (value.startsWith(AlvisConstants.COMMAND_PREFIX) && value.matches("""!connect.+\?.+"""))
                historyBuffer += value.substring(0, value.indexOf("?"))
            else
                historyBuffer += value
        })
        historyBuffer.zipWithIndex.foreach(e => s"${e._2 + 1}\t${e._1}".prettyPrintln(PRINT_RENDER.stringValue))
    }

    def log(line: String): Unit = {
        val args = line.split(" ").map(_.trim).filter(_.nonEmpty).drop(1).++(Array("-p", "isInteractive", "true"))
        logViewer.display(CliDescriptor.createCli(config, args))
        //    val result = Try(LogViewer.display(CliDescriptor.createCli(args)))
        //    if (result.isFailure)
        //      (result.failed.get.getMessage + lineSeparator + Messages.`usage-log")).prettyPrintln(PRINT_RENDER.stringValue)
    }

    def null2Empty(line: String): Unit = this.set(s"set ${line.replace("null2Empty", "print.null2empty")}")

    def set(line: String): Unit = {
        val parts = line.split("= ".toCharArray).map(_.trim).filter(_.notNullAndEmpty)
        if (parts.length != 3)
            Messages.`usage-set`.prettyPrintln(PRINT_RENDER.stringValue)
        else {
            parts(1) match {
                case "print.format" => outputFormat("outputFormat " + parts(2))
                case _ => config.addProperty(parts(1), parts(2))
            }
        }
    }

    def outputFormat(line: String): Unit = {
        val parts = line.split("= ".toCharArray).map(_.trim).filter(_.notNullAndEmpty)
        if (parts.length != 2)
            Messages.`unknown-format`(parts.drop(1).mkString(" "), AlvisConstants.REGISTERED_FORMATS.keys.mkString(", ")).prettyPrintln(PRINT_RENDER.stringValue)
        else
            formatter.changeFormat(parts(1))
    }

    def quit(line: String): Unit = {
        this.closeAll(null)
        sys.exit(0)
    }

    def closeAll(line: String): Unit = {
        DatabaseConnections.setIndex(DatabaseConnections.getConnections.length - 1)
        while (DatabaseConnections.getConnections.nonEmpty)
            this.close(null)
    }

    def close(line: String): Unit = {
        if (DatabaseConnections.current.isNull)
            Messages.`no-current-connection`.prettyPrintln(PRINT_RENDER.stringValue)
        else {
            Messages.`closing`(new Integer(DatabaseConnections.index), DatabaseConnections.current.url).prettyPrintln(PRINT_RENDER.stringValue)
            DatabaseConnections.remove()
        }
    }

    def reconnect(line: String): Unit = {
        if (DatabaseConnections.current.isNull)
            Messages.`no-current-connection`.prettyPrintln(PRINT_RENDER.stringValue)
        else {
            val url = DatabaseConnections.current.url
            Messages.`reconnecting`(url).prettyPrintln(PRINT_RENDER.stringValue)
            val properties = DatabaseConnections.current.properties
            DatabaseConnections.remove()
            DatabaseConnections.create(url, properties)
        }
    }

    def register(line: String): Unit = {
        if (line.matches("register (.*?) as (.*?)$")) {
            val parts = line.split(" ").map(_.trim).filter(_.nonEmpty)
            sparkHBaseHandler.df(HBaseCatalog(parts(1))).createOrReplaceGlobalTempView(parts(3))
        } else {
            Messages.`usage-register`.prettyPrintln(PRINT_RENDER.stringValue)
        }
    }

    def showFunctions(line: String): Unit = {
        import configuredConversions._
        if (DatabaseConnections.current.isNull)
            this.handleQuery("show functions;")
        else {
            DatabaseConnections.current.functions.map(List(_)).prettyShow(columns = List("function"))
        }
    }

    def handleQuery(line: String): Unit = {
        if (DatabaseConnections.index == -2)
            HBaseCommands(alvis).invoke(line)
        else
            this.queryRDB(line)
    }

    def queryRDB(line: String): Unit = {
        val lines = ListBuffer(line.trimComment)
        while (!lines.last.endsWith(";")) {
            val prompt = getPrompt
            val newLine = readLine(" " * prompt.indexOf(">") + ">  ")
            if (!newLine.isComment)
                lines += newLine.trimComment
        }
        val squeeze = ScriptAnalyser.squeeze(lines.toArray)
        squeeze.map(_ + ";").foreach(addHistory)
        ScriptAnalyser.analyse(squeeze, config.getProperties, squeeze = true)
            .foreach {
                s =>
                    query.interactiveQuery(s.split(1.toChar.toString).mkString("\n"))
            }
    }

    def tables(line: String): Unit = {
        val parts = line.split(" ").map(_.trim).filter(_.nonEmpty).drop(1)
        if (parts.length > 0)
            if (DatabaseConnections.index == -1) {
                val tryResult = Try {
                    val start = System.currentTimeMillis()
                    val df = SparkSQL.sql("show tables").toDF("tableName", "isTemporary").filter(_.getAs[String]("tableName").matches(parts(0)))
                    val count = formatter.print(df)
                    val end = System.currentTimeMillis()
                    TinyUtils.timeCost(start, end, count)
                }
                if (tryResult.isFailure)
                    ExceptionHandler.handle(tryResult.failed.get)
            } else {
                this.handleQuery("show tables like '" + parts(0).replace(".+", "%").replace(".*", "%").replace("*", "%") + "';")
            }
        else
            this.handleQuery("show tables;")
    }

}
package org.sa.alvis.connection

import java.sql.DatabaseMetaData
import java.util.Properties

import org.sa.alvis.common.AlvisConstants
import org.sa.utils.universal.config.Config
import org.sa.utils.universal.implicits.SeqConversions._

import scala.collection.mutable.ListBuffer

/**
 * Created by Stuart Alex on 2017/9/8.
 */
object DatabaseConnections {
    private val connections = ListBuffer[DatabaseConnection]()
    var hbaseDatabase = ""
    var hiveDatabase = "/default"
    private var index_ = -1

    def connectionExamples(config: Config): Seq[String] = this.urlExamples(config).join(List("connect", "open")).map(e => AlvisConstants.COMMAND_PREFIX + e._1 + " " + e._2)

    def urlExamples(config: Config): List[String] = List(
        "jdbc:JSQLConnect://<hostname>/database=<database>",
        "jdbc:cloudscape:<database>;create=true",
        "jdbc:twtds:sqlserver://<hostname>/<database>",
        "jdbc:daffodilDB_embedded:<database>;create=true",
        "jdbc:datadirect:db2://<hostname>:50000;databaseName=<database>",
        "jdbc:inetdae:<hostname>:1433",
        "jdbc:datadirect:oracle://<hostname>:1521;SID=<database>;MaxPooledStatements=0",
        "jdbc:datadirect:sqlserver://<hostname>:1433;SelectMethod=cursor;DatabaseName=<database>",
        "jdbc:datadirect:sybase://<hostname>:5000",
        "jdbc:db2://<hostname>/<database>",
        "jdbc:hive2://<hostname>",
        "jdbc:hsqldb:<database>",
        "jdbc:idb:<database>.properties",
        "jdbc:informix-sqli://<hostname>:1526/<database>:INFORMIXSERVER=<database>",
        "jdbc:interbase://<hostname>//<database>.gdb",
        "jdbc:microsoft:sqlserver://<hostname>:1433;DatabaseName=<database>;SelectMethod=cursor",
        "jdbc:mysql://<hostname>/<database>?autoReconnect=true",
        "jdbc:oracle:thin:@<hostname>:1521:<database>",
        "jdbc:pointbase:<database>,database.home=<database>,create=true",
        "jdbc:postgresql://<hostname>:5432/<database>",
        "jdbc:postgresql:net//<hostname>/<database>",
        "jdbc:sybase:Tds:<hostname>:4100/<database>?ServiceName=<database>",
        "jdbc:weblogic:mssqlserver4:<database>@<hostname>:1433",
        "jdbc:odbc:<database>",
        "jdbc:sequelink://<hostname>:4003/[Oracle]",
        "jdbc:sequelink://<hostname>:4004/[Informix];Database=<database>",
        "jdbc:sequelink://<hostname>:4005/[Sybase];Database=<database>",
        "jdbc:sequelink://<hostname>:4006/[SQLServer];Database=<database>",
        "jdbc:sequelink://<hostname>:4011/[ODBC MS Access];Database=<database>",
        "jdbc:openlink://<hostname>/DSN=SQLServerDB/UID=sa/PWD=",
        "jdbc:solid://<hostname>:<port>/<UID>/<PWD>",
        "jdbc:dbaw://<hostname>:8889/<database>") ++
        config.keys.filter(_.matches("""connection..+.url"""))
            .map(_.replace("connection.", "").replace(".url", ""))
            .map(AlvisConstants.REFERENCE_PREFIX + _)

    def getConnections: ListBuffer[DatabaseConnection] = this.connections

    def create(url: String, properties: Properties = new Properties): Unit = {
        properties.putIfAbsent("useunicode", "true")
        properties.putIfAbsent("characterEncoding", "utf8")
        properties.putIfAbsent("autoReconnect", "true")
        properties.putIfAbsent("failOverReadOnly", "false")
        properties.putIfAbsent("zeroDateTimeBehavior", "convertToNull")
        properties.putIfAbsent("transformedBitIsBoolean", "true")
        properties.putIfAbsent("tinyInt1isBit", "false")
        val databaseConnection = new DatabaseConnection(url, properties)
        if (this.connections.indexOf(databaseConnection) == -1)
            this.connections += databaseConnection
        this.index_ = this.connections.indexOf(databaseConnection)
    }

    def iterator: Iterator[DatabaseConnection] = this.connections.iterator

    def length: Int = this.connections.length

    def methods: Array[String] = classOf[DatabaseMetaData].getDeclaredMethods.map(_.getName)

    def remove(index: Int = this.index): Unit = {
        if (index > -1 && index < this.connections.length) {
            this.current.connection.close()
            this.connections.remove(index)
        }
        if (this.index >= this.connections.length)
            this.index_ = this.size - 1
    }

    def current: DatabaseConnection = if (this.index > -1) this.connections(this.index) else null

    def index: Int = this.index_

    def size: Int = this.connections.size

    def setIndex(index: Int): Boolean = {
        if (index == -1 || index == -2 || index < this.connections.size) {
            this.index_ = index
            true
        } else {
            false
        }
    }

}

package org.sa.alvis.component

import java.sql.Statement

import org.sa.alvis.common.Messages
import org.sa.alvis.connection.DatabaseConnections
import org.sa.utils.spark.sql.SparkSQL
import org.sa.utils.universal.base.Mathematics
import org.sa.utils.universal.feature.Heartbeat
import org.sa.utils.universal.implicits.BasicConversions._
import sun.misc.{Signal, SignalHandler}

import scala.util.Try

/**
 * @author StuartAlex on 2019-07-30 15:27
 */
case class Query(alvis: Alvis) extends SignalHandler with Heartbeat {
    var forceExit = false
    Signal.handle(new Signal("INT"), this)
    private var statement: Statement = _
    private var aborted = false

    import alvis._

    def daemonQuery(sql: String, engine: String) = {
        engine match {
            case "default" => SparkSQL.sql(sql)
            case _ =>
                val statement = DatabaseConnections.current.connection.createStatement()
                val hasResult = statement.execute(sql)
                if (hasResult)
                    statement.getResultSet
                else
                    None
        }
    }

    def interactiveQuery(sql: String): Unit = {
        this.aborted = false
        if (DatabaseConnections.index > -1) {
            this.viaJDBC(sql)
        } else {
            this.viaSpark(sql)
        }
    }

    def viaJDBC(sql: String): Unit = {
        this.forceExit = false
        if (DatabaseConnections.current.connection.isClosed) {
            val url = if (DatabaseConnections.current.url.contains("?"))
                DatabaseConnections.current.url.substring(0, DatabaseConnections.current.url.indexOf("?"))
            else
                DatabaseConnections.current.url
            Messages.`closed`(url).prettyPrintln()
        } else {
            val start = System.currentTimeMillis()
            var count = (0, 0)
            if (sql.trim.startsWith("use ")) {
                val url = DatabaseConnections.current.url
                val cd = DatabaseConnections.current.currentDatabase
                DatabaseConnections.current.currentDatabase = if (url.contains("presto")) {
                    val cod = sql.trim.replace("use ", "").trim.replace("`", "")
                    if (cod.contains("."))
                        cod.replace(".", "/")
                    else if (cd.filter(_ == '/').length == 2)
                        cd.split("/")(1) + "/" + cod
                    else
                        cod
                } else {
                    DatabaseConnections.current.connection.createStatement().execute(sql)
                    sql.trim.replace("use ", "").trim
                }
            } else {
                statement = DatabaseConnections.current.connection.createStatement()
                val hasResult = statement.execute(sql)
                if (hasResult) {
                    this.startHeartbeat("Query")
                    val end = System.currentTimeMillis()
                    val resultSet = statement.getResultSet
                    this.stopHeartbeat()
                    if (!this.aborted) {
                        count = formatter.print(resultSet)
                        TinyUtils.timeCost(start, end, count)
                    }
                } else {
                    val end = System.currentTimeMillis()
                    val effected = Messages.`row-effected`(statement.getUpdateCount)
                    val elapsed = Messages.`time-elapsed`(Mathematics.floor((end - start) / 1000d, 3).toString.trimEnd("0").trimEnd("."))
                    (effected + elapsed).prettyPrintln()
                }
            }
            this.stopHeartbeat()
        }
    }

    def viaSpark(sql: String): Unit = {
        this.forceExit = false
        val start = System.currentTimeMillis()
        if (sql.trim.startsWith("use ")) {
            DatabaseConnections.hiveDatabase = "/" + sql.trim.replace("use ", "").trim
            SparkSQL.sql(sql)
        } else if (sql.trim.startsWith("select") || sql.trim.startsWith("show") || sql.trim.startsWith("desc")) {
            val data = SparkSQL.sql(sql)
            val end = System.currentTimeMillis()
            TinyUtils.timeCost(start, end, formatter.print(data))
        } else {
            SparkSQL.sql(sql)
            val end = System.currentTimeMillis()
            val elapsed = Messages.`time-elapsed`(Mathematics.floor((end - start) / 1000d, 3).toString.trimEnd("0").trimEnd("."))
            elapsed.trimStart("(").trimEnd(")").prettyPrintln()
        }
    }

    override def handle(signal: Signal) = {
        this.stopHeartbeat()
        Try {
            if (DatabaseConnections.index > -1 && this.statement != null && !this.statement.isClosed) {
                Messages.`query-aborted`.prettyPrintln()
                this.statement.cancel()
                this.statement.close()
                this.aborted = true
            } else if (DatabaseConnections.index == -1) {
                Messages.`query-aborted`.prettyPrintln()
                sparkContext.cancelAllJobs()
            } else {
                if (!this.forceExit) {
                    Messages.`press-again`.prettyPrintln()
                    this.forceExit = true
                } else {
                    Messages.`force-exit`.prettyPrintln()
                    sys.exit(127)
                }
            }
        }
    }

}

package org.sa.alvis.completer

import java.util

import jline.console.completer._
import org.sa.alvis.common.AlvisConstants
import org.sa.alvis.component.{CommandHandler, HBaseCommands}
import org.sa.alvis.connection.DatabaseConnections
import org.sa.utils.spark.sql.SparkSQL
import org.sa.utils.universal.config.Config
import org.sa.utils.universal.implicits.ArrayConversions._
import org.sa.utils.universal.implicits.BasicConversions._

import scala.collection.JavaConversions._
import scala.util.Try

/**
 * Created by Stuart Alex on 2017/9/13.
 */
class AlvisCompleter(config: Config) extends Completer {
    lazy val supportedHBaseCommands: Array[String] = HBaseCommands.getClass.getMethods
        .filter(_.getParameterCount == 1)
        .filter(_.getParameterTypes()(0) == classOf[Array[String]])
        .map(_.getName)
        .ascending
    private val aggregateCompleter = new AggregateCompleter(
        CommandHandler.handlers(config).flatMap(handler => handler.commands.map(command => {
            val commandCompleter = new StringsCompleter(AlvisConstants.COMMAND_PREFIX + command)
            new AggregateCompleter(handler.completerList.:+(commandCompleter): _*)
        }))
    )

    override def complete(buffer: String, cursor: Int, candidates: util.List[CharSequence]): Int = {
        if (buffer.notNull) {
            val tryResult = Try {
                if (buffer.startsWith(AlvisConstants.COMMAND_PREFIX)) {
                    if (buffer.contains(" ")) {
                        //complete command, display parameter candidates
                        if (buffer.startsWith(AlvisConstants.COMMAND_PREFIX + "connect " + AlvisConstants.REFERENCE_PREFIX)) {
                            val urls = config
                                .keys
                                .filter(_.matches("""connection..+.url"""))
                                .map(_.replace("connection.", "").replace(".url", ""))
                                .map(AlvisConstants.COMMAND_PREFIX + "connect " + AlvisConstants.REFERENCE_PREFIX + _)
                            new StringsCompleter(urls).complete(buffer, cursor, candidates)
                        } else {
                            this.newCompleter(buffer.substring(AlvisConstants.COMMAND_PREFIX.length).split(" ").head).complete(buffer, cursor, candidates)
                        }
                    } else {
                        //incomplete command, display command candidates
                        this.aggregateCompleter.complete(buffer, cursor, candidates)
                    }
                }
                else if (buffer.startsWith("./")) {
                    //file command, display file names in current directory
                    new FileNameCompleter().complete(buffer, cursor, candidates)
                }
                else if (buffer.trim.endsWith("from")) {
                    DatabaseConnections.current.tableCompleter.complete(buffer, cursor, candidates)
                } else if (buffer.trim.startsWith("select")) {
                    DatabaseConnections.current.columnCompleter.complete(buffer, cursor, candidates)
                } else if (DatabaseConnections.index == -2) {
                    new StringsCompleter(supportedHBaseCommands: _*).complete(buffer, cursor, candidates)
                } else {
                    -1
                }
            }
            if (tryResult.isFailure) {
                -1
            } else {
                tryResult.get
            }
        }
        else {
            -1
        }
    }

    private def newCompleter(name: String): Completer = {
        val found = CommandHandler.registeredCommands(config).find(_._1.contains(name)).map(_._2.asInstanceOf[StringsCompleter])
        if (found.isDefined) {
            new Completer() {

                override def complete(buffer: String, cursor: Int, candidates: util.List[CharSequence]): Int = {
                    name match {
                        case "columns" =>
                            val tables = if (DatabaseConnections.current.isNull)
                                SparkSQL.sql("show tables").collect().map(_.getString(0)).toList
                            else {
                                DatabaseConnections.current.tables
                            }
                            new StringsCompleter(tables.map(AlvisConstants.COMMAND_PREFIX + name + " " + _)).complete(buffer, cursor, candidates)
                        case "get" | "set" => new StringsCompleter(config.keys.map(AlvisConstants.COMMAND_PREFIX + name + " " + _)).complete(buffer, cursor, candidates)
                        case _ => new StringsCompleter(found.get.getStrings.map(AlvisConstants.COMMAND_PREFIX + name + " " + _)).complete(buffer, cursor, candidates)
                    }
                }

            }
        } else {
            new NullCompleter
        }
    }

}
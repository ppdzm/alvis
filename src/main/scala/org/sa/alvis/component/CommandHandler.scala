package org.sa.alvis.component

import jline.console.completer.{Completer, NullCompleter, StringsCompleter}
import org.sa.alvis.common.{AlvisConstants, Messages}
import org.sa.alvis.completer.BooleanCompleter
import org.sa.alvis.connection.DatabaseConnections
import org.sa.utils.universal.config.Config
import org.sa.utils.universal.implicits.BasicConversions._

import scala.collection.JavaConversions._

/**
 * Created by Stuart Alex on 2017/9/7.
 */
object CommandHandler {
    def registeredCommands(config: Config): Map[List[String], StringsCompleter] = Map(
        List("alter") -> null,
        List("close") -> null,
        List("closeAll") -> null,
        List("columns") -> null,
        List("compare") -> null,
        List("connect", "open") -> new StringsCompleter(DatabaseConnections.urlExamples(config): _*),
        List("execute") -> null,
        List("get") -> new StringsCompleter(config.keys: _*),
        List("generate") -> null,
        List("go", "#") -> null,
        List("help", "?") -> null,
        List("history") -> null,
        List("list") -> null,
        List("log") -> null,
        List("null2Empty") -> BooleanCompleter,
        List("outputFormat") -> new StringsCompleter(AlvisConstants.REGISTERED_FORMATS.keys.toList),
        List("quit", "done", "exit") -> null,
        List("reconnect") -> null,
        List("register") -> null,
        List("showFunctions") -> null,
        List("set") -> new StringsCompleter(config.keys),
        List("tables") -> null)

    def handlers(config: Config): List[CommandHandler] = {
        this.registeredCommands(config)
            .map {
                e =>
                    if (e._2.isNull)
                        CommandHandler(e._1, List(this.nullCompleter))
                    else
                        CommandHandler(e._1, List(e._2.asInstanceOf[Completer], this.nullCompleter))
            }
            .toList
    }

    private val nullCompleter = new NullCompleter

}

case class CommandHandler(commands: List[String], completerList: List[Completer]) {

    def help: String = Messages.`help`(commands.head)

    def matches(line: String): String = this.commands.find(_.startsWith(line.split(" ").head)).orNull

}

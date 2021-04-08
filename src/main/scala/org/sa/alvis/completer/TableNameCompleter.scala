package org.sa.alvis.completer

import java.util

import jline.console.completer.{Completer, StringsCompleter}
import org.sa.alvis.connection.DatabaseConnections
import org.sa.utils.universal.implicits.BasicConversions._

/**
 * Created by Stuart Alex on 2017/9/7.
 */
object TableNameCompleter extends Completer {

    override def complete(buffer: String, cursor: Int, candidates: util.List[CharSequence]): Int = {
        if (DatabaseConnections.current.isNull || DatabaseConnections.current.isClosed)
            -1
        else
            new StringsCompleter(DatabaseConnections.current.tables: _*).complete(buffer, cursor, candidates)
    }

}
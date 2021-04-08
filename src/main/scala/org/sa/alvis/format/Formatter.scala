package org.sa.alvis.format

import java.sql.ResultSet

import org.apache.spark.sql.DataFrame
import org.sa.alvis.common.{AlvisConstants, Messages}
import org.sa.alvis.component.Alvis
import org.sa.utils.universal.cli.CliUtils
import org.sa.utils.universal.config.ConfigItems._
import org.sa.utils.universal.implicits.BasicConversions._

/**
 * Created by Stuart Alex on 2017/9/11.
 */
case class Formatter(alvis: Alvis) {

    import alvis._

    def print(dataFrame: DataFrame): (Int, Int) = {
        this.print(AlvisConstants.REGISTERED_FORMATS(PRINT_FORMAT.stringValue).format(dataFrame))
    }

    def print(resultSet: ResultSet): (Int, Int) = {
        this.print(AlvisConstants.REGISTERED_FORMATS(PRINT_FORMAT.stringValue).format(resultSet))
    }

    def print(data: (String, Seq[String], String)): (Int, Int) = {
        val (header, rows, footer) = data
        if (header.nonEmpty)
            header.prettyPrintln()
        val length = rows.length
        var count = 0
        var input = ""
        val pageSize = PRINT_PAGE_SIZE.intValue
        while (input != "q" && length > count) {
            val r = rows(count)
            r.prettyPrintln()
            count += 1
            if (pageSize > 0 && count % pageSize == 0 && length > count) {
                input = readLine(Messages.`continue-on-input`).toLowerCase
                CliUtils.deleteRowsUpward(1)
            }
        }
        if (footer.nonEmpty)
            footer.prettyPrintln()
        (rows.size, count)
    }

    def changeFormat(newFormat: String): Unit = {
        if (!AlvisConstants.REGISTERED_FORMATS.contains(newFormat))
            Messages.`unknown-format`(newFormat, AlvisConstants.REGISTERED_FORMATS.keys.mkString(", ")).prettyPrintln()
        else
            PRINT_FORMAT.newValue(newFormat)
    }

}
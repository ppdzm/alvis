package org.sa.alvis.functions

import org.apache.commons.cli.CommandLine
import org.apache.spark.sql.DataFrame
import org.sa.alvis.component.{Alvis, CliDescriptor}
import org.sa.alvis.options.minor.EmailOption
import org.sa.alvis.options.minor.EmailOption.{EssentialParameters => EP}
import org.sa.utils.spark.implicits.DataFrameConversions._
import org.sa.utils.universal.base.MailAgent
import org.sa.utils.universal.implicits.BasicConversions._

/**
 * Created by Stuart Alex on 2017/8/7.
 */
case class Informer(alvis: Alvis) {

    import alvis._

    /**
     * 邮件通知
     *
     * @param commandLine CommandLine
     * @param dataFrame   DataFrame
     * @return
     */
    def inform(commandLine: CommandLine, dataFrame: DataFrame) = {
        if (commandLine.hasOption(EmailOption.name)) {
            EP.validateParameters(config)
            val subject = s"【${CliDescriptor.programName}】表结构比较结果"
            val to = EP.to.getValue(config)
            val html = dataFrame.toHtmlTable
            // MailAgent.send(subject, html, to.split(","))
            "E-mail succeeded sent".prettyPrintln()
        } else {
            val collection = dataFrame.collect()
            if (collection.length > 0)
                dataFrame.prettyShow()
            else
                "No differences found".prettyPrintln()
        }
    }

}

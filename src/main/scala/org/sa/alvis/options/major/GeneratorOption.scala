package org.sa.alvis.options.major

import org.sa.alvis.common.Messages
import org.sa.utils.universal.cli.{CommonOption, CommonParameter}

/**
 * Created by Stuart Alex on 2017/8/4.
 */
object GeneratorOption extends CommonOption("g", "generate") {
    private val hasArg = false

    def option = this.createOption(this.hasArg, this.description)

    private def description = Messages.`description-generator`(this.EssentialParameters)

    object EssentialParameters extends CommonParameter {
        type EssentialParameters = Value
        val namespace = Value("hbase.namespace")
        val table = Value("hbase.table")
        val rowkeyAlias = Value("hbase.rowkey.alias")
        val columns = Value("hbase.columns")

        override def defaultValues = super.defaultValues ++ Map(this.namespace -> "default", this.rowkeyAlias -> "rowkey")

    }

}
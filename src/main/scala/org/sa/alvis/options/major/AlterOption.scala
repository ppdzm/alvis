package org.sa.alvis.options.major

import org.apache.commons.cli.{Option, OptionGroup}
import org.sa.alvis.common.Messages
import org.sa.utils.universal.cli.{CommonOption, CommonParameter}
import org.sa.utils.universal.feature.ExceptionGenerator

/**
 * Created by Stuart Alex on 2017/8/4.
 */
object AlterOption extends CommonOption("a", "alter") {
    lazy val subOptionNotProvidedException = ExceptionGenerator.newException("PropertyNotExist", s"$AddOption or $DropOption option must be provided !")
    private val hasArg = false

    def option: Option = this.createOption(this.hasArg, this.description)

    private def description = Messages.`description-alter`(this.EssentialParameters)

    def optionGroup: OptionGroup = new OptionGroup()
        .addOption(AddOption.option)
        .addOption(DropOption.option)

    object EssentialParameters extends CommonParameter {
        type AlterEssentialParameters = Value
        val hiveDatabase = Value("hive.database")
        val hiveTable = Value("hive.table")
        val hiveColumnName = Value("hive.column.name")
        val hiveColumnType = Value("hive.column.type")
        val hbaseColumnFamilyName = Value("hbase.column.family.name")
        val hbaseColumnName = Value("hbase.column.name")
    }

    object AddOption extends CommonOption("A", "add") {

        private val hasArg = false

        def option: Option = this.createOption(this.hasArg, this.description)

        private def description = Messages.`description-alter-add`(AlterOption)
    }

    object DropOption extends CommonOption("D", "drop") {
        private val hasArg = false

        def option: Option = this.createOption(this.hasArg, this.description)

        private def description = Messages.`description-alter-drop`(AlterOption)
    }

}
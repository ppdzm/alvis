package org.sa.alvis.options.major

import org.apache.commons.cli.OptionGroup
import org.sa.alvis.common.Messages
import org.sa.utils.universal.cli.{CommonOption, CommonParameter}

/**
 * Created by Stuart Alex on 2017/8/4.
 */
object CompareOption extends CommonOption("c", "compare") {
    private val hasArg = false

    def option = this.createOption(this.hasArg, this.compareDescription)

    private def compareDescription = Messages.`description-compare`(CompareDatabaseOption, CompareFileOption, EssentialParameters)

    def optionGroup = new OptionGroup()
        .addOption(CompareDatabaseOption.option)
        .addOption(CompareFileOption.option)

    object EssentialParameters extends CommonParameter {
        type EssentialParameters = Value
        val mysqlUrl = Value("compare.mysql.url")
        val mysqlTable = Value("compare.mysql.table")
        val hiveDatabase = Value("compare.hive.database")
        val hiveTable = Value("compare.hive.table")
    }

    object CompareDatabaseOption extends CommonOption("B", "database") {
        private val hasArg = false

        def option = this.createOption(this.hasArg, this.description)

        private def description = Messages.`description-compare-database`(CompareOption, this.EssentialParameters)

        object EssentialParameters extends CommonParameter {
            type EssentialParameters = Value
            val mysqlUrl = Value("compare.mysql.url")
            val mysqlTable = Value("compare.mysql.table")
            val mysqlUrlColumn = Value("column.mysql.url")
            val mysqlTableColumn = Value("column.mysql.table")
            val hiveDatabaseColumn = Value("column.hive.database")
            val hiveTableColumn = Value("column.hive.table")
        }

    }

    object CompareFileOption extends CommonOption("F", "file") {
        private val hasArg = false

        def option = this.createOption(this.hasArg, this.description)

        private def description = Messages.`description-compare-file`(CompareOption, HeaderOption, IndexOption)

        def optionGroup = new OptionGroup()
            .addOption(CompareFileOption.HeaderOption.option)
            .addOption(CompareFileOption.IndexOption.option)

        object HeaderOption extends CommonOption("H", "header") {
            private val hasArg = false

            def option = this.createOption(this.hasArg, this.description)

            private def description = Messages.`description-compare-file-header`(CompareFileOption, this.EssentialParameters)

            object EssentialParameters extends CommonParameter {
                type EssentialParameters = Value
                val file = Value("compare.file")
                val delimiter = Value("compare.file.delimiter")
                val mysqlUrlColumn = Value("column.mysql.url")
                val mysqlTableColumn = Value("column.mysql.table")
                val hiveDatabaseColumn = Value("column.hive.database")
                val hiveTableColumn = Value("column.hive.table")
            }

        }

        object IndexOption extends CommonOption("I", "index") {
            private val hasArg = false

            def option = this.createOption(this.hasArg, this.description)

            private def description = Messages.`description-compare-file-index`(CompareFileOption, this.EssentialParameters)

            object EssentialParameters extends CommonParameter {
                type EssentialParameters = Value
                val file = Value("compare.file")
                val delimiter = Value("compare.file.delimiter")
                val mysqlUrlIndex = Value("index.mysql.url")
                val mysqlTableIndex = Value("index.mysql.table")
                val hiveDatabaseIndex = Value("index.hive.database")
                val hiveTableIndex = Value("index.hive.table")
            }

        }

    }

    object MappingOption extends CommonOption("M", "mapping") {
        private val hasArg = true

        def option = this.createOption(this.hasArg, this.description).argName(this.argument)

        private def description = Messages.`description-compare-mapping`(CompareOption)

        private def argument = Messages.`argument-compare-mapping`
    }

}
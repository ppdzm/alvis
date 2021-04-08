package org.sa.alvis.functions

import org.apache.commons.cli.CommandLine
import org.sa.alvis.component.Alvis
import org.sa.alvis.options.major.AlterOption
import org.sa.alvis.options.major.AlterOption.{EssentialParameters => EP}
import org.sa.utils.spark.implicits.DataFrameConversions._
import org.sa.utils.spark.hive.SparkHiveUtils
import org.sa.utils.spark.sql.SparkSQL
import org.sa.utils.universal.implicits.BasicConversions._

/**
 * Created by Stuart Alex on 2017/7/26.
 */
case class TableModifier(alvis: Alvis) {
    private val columns = Array("col_name", "data_type")

    import alvis._

    /**
     * 修改Hive外部表结构
     *
     * @param commandLine CommandLine
     */
    def alter(commandLine: CommandLine): Unit = {
        EP.validateParameters(config)
        if (commandLine.hasOption(AlterOption.AddOption.name)) {
            add(commandLine)
        } else if (commandLine.hasOption(AlterOption.DropOption.name)) {
            drop(commandLine)
        } else {
            throw AlterOption.subOptionNotProvidedException
        }
    }

    /**
     * 增加单个字段
     *
     * @param commandLine CommandLine
     */
    private def add(commandLine: CommandLine): Unit = {
        val database = EP.hiveDatabase.getValue(config)
        val table = EP.hiveTable.getValue(config)
        val columnName = EP.hiveColumnName.getValue(config)
        val columnType = EP.hiveColumnType.getValue(config)
        val columnFamily = EP.hbaseColumnFamilyName.getValue(config)
        val hColumnName = EP.hbaseColumnName.getValue(config)
        if (SparkHiveUtils.isExternalHiveOverHBaseTable(database, table)) {
            "Before add column:".prettyPrintln()
            SparkSQL.sql(s"desc $database.$table").prettyShow()
        }
        SparkHiveUtils.addColumn2ExternalHiveOverHBaseTable(database, table, columnName, columnType, columnFamily, hColumnName)
        "After add column:".prettyPrintln()
        SparkSQL.sql(s"desc $database.$table").prettyShow()
    }

    /**
     * 删除单个字段
     *
     * @param commandLine CommandLine
     */
    private def drop(commandLine: CommandLine): Unit = {
        val database = EP.hiveDatabase.getValue(config)
        val table = EP.hiveTable.getValue(config)
        val columnName = EP.hiveColumnName.getValue(config)
        val columnType = EP.hiveColumnType.getValue(config)
        val columnFamily = EP.hbaseColumnFamilyName.getValue(config)
        val hColumnName = EP.hbaseColumnName.getValue(config)
        if (SparkHiveUtils.isExternalHiveOverHBaseTable(database, table)) {
            "Before drop column:".prettyPrintln()
            SparkSQL.sql(s"desc $database.$table").prettyShow()
        }
        SparkHiveUtils.dropColumnFromExternalHiveOverHBaseTable(database, table, columnName, columnType, columnFamily, hColumnName)
        "After drop column:".prettyPrintln()
        SparkSQL.sql(s"desc $database.$table").prettyShow()
    }

}

package org.sa.alvis.functions

import java.io.File

import org.apache.commons.cli.CommandLine
import org.sa.alvis.component.Alvis
import org.sa.alvis.options.major.CompareOption
import org.sa.alvis.options.major.CompareOption.CompareDatabaseOption.{EssentialParameters => DEP}
import org.sa.alvis.options.major.CompareOption.CompareFileOption.HeaderOption.{EssentialParameters => FHEP}
import org.sa.alvis.options.major.CompareOption.CompareFileOption.IndexOption.{EssentialParameters => FIEP}
import org.sa.alvis.options.major.CompareOption.{CompareDatabaseOption, CompareFileOption, MappingOption, EssentialParameters => EP}
import org.sa.utils.spark.implicits.SeqLikeConversions._
import org.sa.utils.spark.file.SparkFile
import org.sa.utils.spark.hive.SparkHiveUtils
import org.sa.utils.spark.sql.SparkSQL
import org.sa.utils.universal.feature.ExceptionGenerator


/**
 * Created by Stuart Alex on 2017/7/26.
 */
case class Comparator(alvis: Alvis) {
    private val columns4Email = Array("MySQL地址", "MySQL表名", "Hive表名", "字段名", "MySQL字段类型", "Hive字段类型", "比较结论")
    private val columns4Persist = Array("mysql_url", "mysql_table_name", "hive_table_name", "column_name", "mysql_column_type", "hive_column_type", "conclusion")

    import alvis._

    /**
     * 比较MySQL源表和Hive目标表结构差异
     *
     * @param commandLine CommandLine
     */
    def compare(commandLine: CommandLine): Unit = {
        import sparkSession.implicits._
        val compareResult = if (commandLine.hasOption(CompareDatabaseOption.name))
            compareFromDatabase(commandLine)
        else if (commandLine.hasOption(CompareFileOption.name))
            compareFromFile(commandLine)
        else
            compareFromCLI(commandLine)
        val rdd = compareResult.parallelize()
        informer.inform(commandLine, rdd.toDF(this.columns4Email: _*))
        exporter.export(commandLine, rdd.toDF(this.columns4Persist: _*))
    }

    /**
     * 从命令行获取配置
     *
     * @param commandLine CommandLine
     * @return
     */
    private def compareFromCLI(commandLine: CommandLine) = {
        EP.validateParameters(config)
        val url = EP.mysqlUrl.getValue(config)
        val mysqlTable = EP.mysqlTable.getValue(config)
        val hiveDatabase = EP.hiveDatabase.getValue(config)
        val hiveTable = EP.hiveTable.getValue(config)
        val mapping = this.getMapping(commandLine)
        SparkHiveUtils.compare(url, mysqlTable, hiveDatabase, hiveTable, mapping)
    }

    /**
     * 从MySQL数据库表获取配置
     *
     * @param commandLine CommandLine
     * @return
     */
    private def compareFromDatabase(commandLine: CommandLine) = {
        DEP.validateParameters(config)
        var list = List[(String, String, String, String, String, String, String)]()
        val url = DEP.mysqlUrl.getValue(config)
        val table = DEP.mysqlTable.getValue(config)
        val mysqlUrlColumn = DEP.mysqlUrlColumn.getValue(config)
        val mysqlTableColumn = DEP.mysqlTableColumn.getValue(config)
        val hiveDatabaseColumn = DEP.hiveDatabaseColumn.getValue(config)
        val hiveTableColumn = DEP.hiveTableColumn.getValue(config)
        val mapping = this.getMapping(commandLine)
        SparkSQL.mysql.df(url, table).select(mysqlUrlColumn, mysqlTableColumn, hiveDatabaseColumn, hiveTableColumn).collect().foreach(row => {
            val url = row.getAs[String](mysqlUrlColumn)
            val mysqlTableRegex = row.getAs[String](mysqlTableColumn)
            val hiveDatabase = row.getAs[String](hiveDatabaseColumn)
            val hiveTable = row.getAs[String](hiveTableColumn)
            list = list.++:(SparkHiveUtils.compare(url, mysqlTableRegex, hiveDatabase, hiveTable, mapping))
        })
        list
    }

    /**
     * 从文件获取配置
     *
     * @param commandLine CommandLine
     * @return
     */
    private def compareFromFile(commandLine: CommandLine) = {
        var list = List[(String, String, String, String, String, String, String)]()
        if (commandLine.hasOption(CompareOption.CompareFileOption.HeaderOption.name)) {
            FHEP.validateParameters(config)
            val file = FHEP.file.getValue(config)
            val delimiter = FHEP.delimiter.getValue(config).toCharArray.head
            val mysqlUrlColumn = FHEP.mysqlUrlColumn.getValue(config)
            val mysqlTableColumn = FHEP.mysqlTableColumn.getValue(config)
            val hiveDatabaseColumn = FHEP.hiveDatabaseColumn.getValue(config)
            val hiveTableColumn = FHEP.hiveTableColumn.getValue(config)
            val mapping = this.getMapping(commandLine)
            val fileConfig = SparkFile.df("file:///" + new File(file).getAbsolutePath, useHeader = true, delimiter)
            fileConfig.collect().foreach(row => {
                val url = row.getAs[String](mysqlUrlColumn)
                val mysqlTableRegex = row.getAs[String](mysqlTableColumn)
                val hiveDatabase = row.getAs[String](hiveDatabaseColumn)
                val hiveTable = row.getAs[String](hiveTableColumn)
                list = list.++:(SparkHiveUtils.compare(url, mysqlTableRegex, hiveDatabase, hiveTable, mapping))
            })
        } else if (commandLine.hasOption(CompareOption.CompareFileOption.IndexOption.name)) {
            FIEP.validateParameters(config)
            val file = FIEP.file.getValue(config)
            val delimiter = FIEP.delimiter.getValue(config).toCharArray.head
            val mysqlUrlIndex = FIEP.mysqlUrlIndex.getValue(config)
            val mysqlTableIndex = FIEP.mysqlTableIndex.getValue(config)
            val hiveDatabaseIndex = FIEP.hiveDatabaseIndex.getValue(config)
            val hiveTableIndex = FIEP.hiveTableIndex.getValue(config)
            val mapping = this.getMapping(commandLine)
            val fileConfig = SparkFile.df("file:///" + new File(file).getAbsolutePath, useHeader = false, delimiter)
            fileConfig.collect().foreach(row => {
                val url = row.getString(mysqlUrlIndex.toInt)
                val mysqlTableRegex = row.getString(mysqlTableIndex.toInt)
                val hiveDatabase = row.getString(hiveDatabaseIndex.toInt)
                val hiveTable = row.getString(hiveTableIndex.toInt)
                list = list.++:(SparkHiveUtils.compare(url, mysqlTableRegex, hiveDatabase, hiveTable, mapping))
            })
        } else {
            throw ExceptionGenerator.newException("PropertyNotExist", "-H/--header or a -I/--index option must be provided !")
        }
        list
    }

    /**
     * 获取免检类型映射
     *
     * @param commandLine CommandLine
     * @return
     */
    private def getMapping(commandLine: CommandLine) = {
        val values = MappingOption.getOptionValues(commandLine)
        if (values.nonEmpty)
            values.filter(_.contains(":")).map(_.split(":")).map(r => r(0) -> r(1)).toMap
        else
            Map[String, String]()
    }

}

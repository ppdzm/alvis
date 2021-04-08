package org.sa.alvis.component

import java.io.File
import java.util.Properties

import com.google.common.base.Charsets
import com.google.common.io.Files
import org.apache.commons.cli.CommandLine
import org.sa.alvis.common.Messages
import org.sa.alvis.connection.DatabaseConnections
import org.sa.alvis.options.major.ScriptOption
import org.sa.alvis.options.minor.EngineOption
import org.sa.utils.hadoop.hbase.HBaseCatalog
import org.sa.utils.spark.sql.SparkSQL
import org.sa.utils.universal.base.Logging
import org.sa.utils.universal.base.Symbols._
import org.sa.utils.universal.cli.{CliUtils, ParameterOption}
import org.sa.utils.universal.feature.ExceptionGenerator
import org.sa.utils.universal.implicits.ArrayConversions._
import org.sa.utils.universal.implicits.BasicConversions._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.util.Try
import scala.util.matching.Regex

/**
 * Created by Stuart Alex on 2017/5/2.
 */
case class ScriptExecutor(commandLine: CommandLine, alvis: Alvis, interactive: Boolean) extends Logging {

    import alvis._

    private lazy val pattern: Regex = """[#$]\{[^#\}$]+\}""".r
    private lazy val filePath =
        if (this.interactive || sparkContext.master != "yarn")
            ScriptOption.getOptionValue(this.commandLine)
        else {
            val value = ScriptOption.getOptionValue(this.commandLine)
            value.substring(value.lastIndexOf(slash) + 1)
        }
    private lazy val file = new File(this.filePath)
    private lazy val properties = this.commandLine.getOptionProperties(ParameterOption.name)
    private lazy val originalScriptLines = Files.readLines(file, Charsets.UTF_8).asScala.toArray
    private lazy val engine = if (this.commandLine.hasOption(EngineOption.longName))
        EngineOption.getOptionValue(this.commandLine)
    else
        "default"

    import alvis._

    /**
     * 执行解析后的脚本
     *
     * @return
     */
    def execute(): Unit = {
        val scripts = validate(analyse(this.originalScriptLines, this.properties, interactive = false))
        if (scripts.length == 0) {
            this.logWarning("Length of scripts is zero !")
        } else {
            if (this.engine != "default")
                DatabaseConnections.create(engineUrl(this.engine).stringValue)
            this.logInfo(s"Scripts after analyse are follow:")
            this.logInfo(scripts.withRowNumber.mkString("\t", s"\n\t", ""))
            this.logInfo(s"Parameters' information are follow:")
            this.logInfo(this.properties.map(p => p._1 + " is set to " + p._2).toArray.ascending.withRowNumber.mkString("\t", s"\n\t", ""))
            if (scripts.nonEmpty) {
                scripts.dropRight(1).foreach(script => {
                    this.logInfo(s"Execute script:")
                    this.logInfo(s"\t$script")
                    if (this.interactive)
                        query.interactiveQuery(script)
                    else
                        query.daemonQuery(script, this.engine)
                })
                this.logInfo(s"Execute script:")
                this.logInfo(s"\t${scripts.last}")
                if (this.interactive)
                    query.interactiveQuery(scripts.last)
                else
                    exporter.export(this.commandLine, query.daemonQuery(scripts.last, this.engine))
            }
        }
    }

    /**
     * 检查语句中是否仍然有参数
     *
     * @param scripts
     * @return
     */
    def validate(scripts: Array[String]): Array[String] = {
        val missing = scripts.filter(this.pattern.findFirstMatchIn(_).isDefined)
        if (missing.length > 0)
            throw ExceptionGenerator.newException("PropertyNotPerfect", s"Parameters does not provided in following scripts:\n${missing.withRowNumber.mkString("\t", s"\n\t", "")}")
        scripts
    }

    /**
     * 解析单条SQL语句
     *
     * @param sql
     * @param properties
     * @return
     */
    def analyse(sql: String, properties: Properties): String = this.analyse(Array(sql), properties, interactive = false).head

    /**
     * 解析多条语句
     *
     * @param originalScriptLines
     * @param properties
     * @return
     */
    def analyse(originalScriptLines: Array[String], properties: Properties, interactive: Boolean): Array[String] = {
        val hiveEnvRegex = "hive_env (?<key>.*?)=(?<value>.*? )$".r
        val sparkEnvRegex = "spark_env (?<key>.*?)=(?<value>.*?)$".r
        //set a:=select max(id) from some_table
        val argumentRegex = "set (?<argument>.*?):=(?<sql>.*?)$".r
        //set a=b
        val variableRegex = "set (?<variable>.*?)=(?<value>.*?)$".r
        val registerRegex = "register (?<json>.*?) as (?<temporary>.*?)$".r
        this.logInfo(s"Get the original scripts are follow:")
        this.logInfo(originalScriptLines.mkString("\t", s"\n\t", ""))
        originalScriptLines
            .map(line => line.trimComment) //去掉每行注释后清除首尾空白字符
            .filter(_.notNullAndEmpty) //筛掉空行
            .mkString(" ") //所有行用空格区分加在一起
            .split(";") //用分号做分隔符，截取成多个语句
            .map(_.trim.replace("hivevar:", "")) //清除首尾空白字符后将${hivevar:variable}替换成${variable}
            .filter {
                case hiveEnvRegex(key, value) =>
                    sparkSession.conf.set(key, value)
                    false
                case sparkEnvRegex(key, value) =>
                    sparkSession.conf.set(key, value)
                    false
                case argumentRegex(_, _) => true
                case registerRegex(_, _) => true
                case variableRegex(variable, value) =>
                    properties.put(variable.trim, value.trim)
                    false
                case _ => true
            } //去掉声明的变量
            .map(this.substitute(_, properties)) //第一次替换变量
            .filter {
                case argumentRegex(argument, sql) => argue(properties, argument, sql)
                case _ => true
            } //第一次尝试去掉赋值的变量
            .map(this.substitute(_, properties)) //第二次替换变量
            .filter {
                case registerRegex(json, temporaryTable) =>
                    val catalog = HBaseCatalog(json.trim)
                    this.logInfo(s"Reading data from ${catalog.table.name} and register as temporary table named $temporaryTable")
                    sparkHBaseHandler.df(catalog).createOrReplaceTempView(temporaryTable.trim)
                    false
                case _ => true
            } //注册HBase表
            .map(this.substitute(_, properties)) //第三次替换变量
            .filter {
                case argumentRegex(argument, sql) => argue(properties, argument, sql)
                case _ => true
            } //第一次尝试去掉赋值的变量
            .map(this.substitute(_, properties)) //第四次替换变量
            .map(_.trim) //清除变量替换完成后每行的首尾空白字符
            .filter(_.notNullAndEmpty) //筛掉空行
            .map(_.recursiveReplace("  ", " "))
            .map(_.recursiveReplace("( ", "(")) //去除所有左括号后的空格
            .map(_.recursiveReplace(" )", ")")) //去除所有右括号前的空格
    }

    def argue(properties: Properties, argument: String, sql: String): Boolean = {
        Try {
            this.logInfo(s"Try set value to $argument by execute sql $sql")
            val value = SparkSQL.sql(sql).collect()(0).get(0).toString
            this.logInfo(s"Got value $value to $argument")
            properties.put(argument.trim, value.trim)
        }.isFailure
    }

    /**
     * 将参数替换为实际值
     *
     * @param script
     * @param properties
     * @return
     */
    def substitute(script: String, properties: Properties, readParameterFromConsole: Boolean = false): String = {
        if (script.isNull)
            null
        else {
            var temp = script
            var canNotFindMore = false
            val listBuffer = ListBuffer[String]()
            var inputParameterCount = 0
            while (this.pattern.findFirstMatchIn(temp).isDefined && !canNotFindMore) {
                canNotFindMore = true
                this.pattern.findAllMatchIn(temp).foreach(m => {
                    val matcher = m.group(0)
                    val name = matcher.substring(2, matcher.length - 1).replace("hivevar:", "")
                    val value = properties.getProperty(name)
                    if (value != null) {
                        temp = temp.replace(matcher, value)
                        canNotFindMore = false
                    } else if (readParameterFromConsole && !listBuffer.contains(name)) {
                        listBuffer += name
                        val parameterValue = readLine(Messages.`parameter-input`(name))
                        inputParameterCount += 1
                        if (parameterValue.notNullAndEmpty) {
                            temp = temp.replace(matcher, parameterValue)
                            properties.put(name, parameterValue)
                        }
                    }
                })
            }
            if (inputParameterCount > 0)
                CliUtils.deleteRowsUpward(inputParameterCount)
            temp
        }
    }

}
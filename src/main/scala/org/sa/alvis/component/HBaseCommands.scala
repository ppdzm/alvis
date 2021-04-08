package org.sa.alvis.component

import java.lang

import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp
import org.apache.hadoop.hbase.filter._
import org.apache.hadoop.hbase.util.Bytes
import org.sa.alvis.common.Messages
import org.sa.alvis.format.DefaultOutputFormat
import org.sa.utils.hadoop.hbase.implicts.HBaseImplicits._
import org.sa.utils.universal.base.StringUtils
import org.sa.utils.universal.cli.CliUtils
import org.sa.utils.universal.config.ConfigItems
import org.sa.utils.universal.feature.Heartbeat
import org.sa.utils.universal.implicits.BasicConversions._

import scala.collection.JavaConverters._
import scala.util.Try

/**
 * Created by Stuart Alex on 2017/11/11.
 */
case class HBaseCommands(alvis: Alvis) extends Heartbeat {

    import alvis._

    def clone(parts: Array[String]): Unit = {
        parts.length match {
            case 3 => hbaseHandler.clone(parts(1), parts(2))
        }
    }

    def count(parts: Array[String]): Unit = {
        val count = parts.length match {
            case 2 => hbaseHandler.count(parts(1))
        }
        this.stopHeartbeat()
        count.toString.prettyPrintln()
    }

    def create(parts: Array[String]): Unit = {
        if (parts.length > 2)
            hbaseHandler.createTable(parts(1), parts.drop(2))
        else
            parts.length match {
                case 3 =>
            }
    }

    def create_namespace(parts: Array[String]): Unit = {
        parts.length match {
            case 2 => hbaseHandler.createNamespace(parts(1))
        }
    }

    def delete(parts: Array[String]): Unit = {
        parts.length match {
            case 3 => hbaseHandler.delete(parts(1), parts(2))
            case 5 => hbaseHandler.delete(parts(1), parts(2), parts(3), parts(4))
            case 6 => hbaseHandler.delete(parts(1), parts(2), parts(3), parts(4), parts(5).toLong)
        }
    }

    def delete_snapshot(parts: Array[String]) = {
        parts.length match {
            case 2 => hbaseHandler.deleteSnapshot(parts(1))
        }
    }

    def disable_and_drop(parts: Array[String]) = {
        this.disable(parts)
        this.drop(parts)
    }

    def disable(parts: Array[String]) = {
        parts.length match {
            case 2 => hbaseHandler.disableTable(parts(1))
        }
    }

    def drop(parts: Array[String]) = {
        parts.length match {
            case 2 => hbaseHandler.dropTable(parts(1))
        }
    }

    def enable(parts: Array[String]) = {
        parts.length match {
            case 2 => hbaseHandler.enableTable(parts(1))
        }
    }

    def get(parts: Array[String]) = {
        parts.length match {
            case 3 => hbaseHandler.get(parts(1), parts(2)).prettyShow()
        }
    }

    def list(parts: Array[String]) = {
        val start = System.currentTimeMillis()
        val (list, title) = parts.length match {
            case 1 => (hbaseHandler.list(), "table")
            case 2 => (hbaseHandler.list(pattern = parts(1)), s"tables_with_pattern_${parts(1)}")
        }
        this.stopHeartbeat()
        val count = formatter.print(DefaultOutputFormat.format(list, title))
        val end = System.currentTimeMillis()
        TinyUtils.timeCost(start, end, count)
    }

    def list_filters(parts: Array[String]) = {
        val help = Array("ColumnCountGetFilter", "ColumnPaginationFilter", "ColumnPrefixFilter", "ColumnRangeFilter",
            "DependentColumnFilter", "FamilyFilter", "FirstKeyOnlyFilter", "InclusiveStopFilter",
            "KeyOnlyFilter", "MultipleColumnPrefixFilter", "PageFilter", "PrefixFilter",
            "QualifierFilter", "RowFilter", "SingleColumnValueExcludeFilter", "SingleColumnValueFilter",
            "TimestampsFilter", "ValueFilter").map(e => (e, Messages.`filter`(e)))
        this.stopHeartbeat()
        CliUtils.printHelp(help)
    }

    def list_namespace(parts: Array[String]) = {
        val start = System.currentTimeMillis()
        this.stopHeartbeat()
        val count = formatter.print(DefaultOutputFormat.format(hbaseHandler.listNamespaces(), "namespace"))
        val end = System.currentTimeMillis()
        TinyUtils.timeCost(start, end, count)
    }

    def list_namespace_tables(parts: Array[String]) = {
        val start = System.currentTimeMillis()
        val (list, title) = parts.length match {
            case 2 => (hbaseHandler.list(namespace = parts(1)), s"table_in_${parts(1)}")
            case 3 => (hbaseHandler.list(parts(1), parts(2)), s"table_in_${parts(1)}_with_pattern_${parts(2)}")
        }
        this.stopHeartbeat()
        val count = formatter.print(DefaultOutputFormat.format(list, title))
        val end = System.currentTimeMillis()
        TinyUtils.timeCost(start, end, count)
    }

    def list_snapshots(parts: Array[String]) = {
        val start = System.currentTimeMillis()
        val list = parts.length match {
            case 1 => hbaseHandler.listSnapshot()
            case 2 => hbaseHandler.listSnapshot(parts(1))
        }
        this.stopHeartbeat()
        val count = formatter.print(DefaultOutputFormat.format(list, List("snapshot", "table", "create_time")))
        val end = System.currentTimeMillis()
        TinyUtils.timeCost(start, end, count)
    }

    def put(parts: Array[String]) = {
        parts.length match {
            case 6 => hbaseHandler.put(parts(1), parts(2), parts(3), Map(parts(4) -> parts(5)))
        }
    }

    def scan(parts: Array[String]) = {
        val s1 = "LIMIT=>"
        val s2 = "PAGE=>"
        val s3 = "FILTER=>"
        val limit = if (parts.length > 2)
            parts.find(_.startsWith(s1)).getOrElse("0").trimStart(s1).toInt
        else
            parts.length match {
                case 2 => 0
            }
        val page = if (parts.length > 2)
            parts.find(_.startsWith(s2)).getOrElse("1").trimStart(s2).toInt
        else
            parts.length match {
                case 2 => 1
            }
        val filterSettings = parts.drop(2).filterNot(_.startsWith(s1)).filterNot(_.startsWith(s2))
        if (filterSettings.nonEmpty && filterSettings.exists(!_.startsWith(s3))) {
            1 match {
                case 2 =>
            }
        }
        val table = parts(1)
        val iterator = if (parts.length == 2) {
            hbaseHandler.scan(table, null)
        } else {
            val filterList = new FilterList()
            filterSettings.map(_.trimStart(s3)).foreach(f => {
                val filterClass = f.substring(0, f.indexOf("("))
                val parameters = StringUtils.split(f.substring(f.indexOf("(") + 1, f.length - 1), ",").filterNot(_.isEmpty)
                val filter = Try(filterClass match {
                    case "ColumnCountGetFilter" => new ColumnCountGetFilter(parameters(0).toInt)
                    case "ColumnPaginationFilter" => new ColumnPaginationFilter(parameters(0).toInt, parameters(1).toInt)
                    case "ColumnPrefixFilter" => new ColumnPrefixFilter(Bytes.toBytes(parameters(0)))
                    case "ColumnRangeFilter" => new ColumnRangeFilter(Bytes.toBytes(parameters(0)), parameters(1).toBoolean, Bytes.toBytes(parameters(2)), parameters(3).toBoolean)
                    case "DependentColumnFilter" => new DependentColumnFilter(Bytes.toBytes(parameters(0)), Bytes.toBytes(parameters(1)), parameters(2).toBoolean, CompareOp.valueOf(parameters(3)), new SubstringComparator(parameters(4)))
                    case "FamilyFilter" => new FamilyFilter(CompareOp.valueOf(parameters(0)), new SubstringComparator(parameters(1)))
                    case "FirstKeyOnlyFilter" => new FirstKeyOnlyFilter()
                    case "InclusiveStopFilter" => new InclusiveStopFilter(Bytes.toBytes(parameters(0)))
                    case "KeyOnlyFilter" => new KeyOnlyFilter()
                    case "MultipleColumnPrefixFilter" => new MultipleColumnPrefixFilter(parameters.map(Bytes.toBytes))
                    case "PageFilter" => new PageFilter(parameters(0).toLong)
                    case "PrefixFilter" => new PrefixFilter(Bytes.toBytes(parameters(0)))
                    case "QualifierFilter" => new QualifierFilter(CompareOp.valueOf(parameters(0)), new SubstringComparator(parameters(1)))
                    case "RowFilter" => new RowFilter(CompareOp.valueOf(parameters(0)), new SubstringComparator(parameters(1)))
                    case "SingleColumnValueExcludeFilter" => new SingleColumnValueExcludeFilter(Bytes.toBytes(parameters(0)), Bytes.toBytes(parameters(1)), CompareOp.valueOf(parameters(2)), Bytes.toBytes(parameters(3)))
                    case "SingleColumnValueFilter" => new SingleColumnValueFilter(Bytes.toBytes(parameters(0)), Bytes.toBytes(parameters(1)), CompareOp.valueOf(parameters(2)), Bytes.toBytes(parameters(3)))
                    case "TimestampsFilter" => new TimestampsFilter(parameters.map(_.toLong.asInstanceOf[lang.Long]).toList.asJava)
                    case "ValueFilter" => new ValueFilter(CompareOp.valueOf(parameters(0)), new SubstringComparator(parameters(1)))
                })
                if (filter.isFailure) {
                    val name = filter.failed.get.getClass.getName
                    val cause = filter.failed.get.getCause
                    if (name == "scala.MatchError" || (cause != null && cause.getClass.getName == "scala.MatchError"))
                        Messages.`unknown-filter`(filterClass).prettyPrintln()
                    else
                        Messages.`filter`(filterClass).prettyPrintln()
                    throw filter.failed.get
                }
                else
                    filterList.addFilter(filter.get)
            })
            if (filterList.getFilters.isEmpty)
                hbaseHandler.scan(table, null)
            else
                hbaseHandler.scan(table, filterList)
        }
        var input = ""
        var count = 0
        var shallContinue = iterator.hasNext
        val oldLinefeed = ConfigItems.PRINT_LINEFEED.intValue
        if (oldLinefeed == 0)
            ConfigItems.PRINT_LINEFEED.newValue(100)
        this.stopHeartbeat()
        while (shallContinue && input.toLowerCase != "q") {
            iterator.next().prettyShow()
            count += 1
            shallContinue = iterator.hasNext && (limit == 0 || count < limit)
            if (shallContinue && count % page == 0) {
                input = readLine(Messages.`continue-on-input`).toLowerCase
                CliUtils.deleteRowsUpward(1)
            }
        }
        ConfigItems.PRINT_LINEFEED.newValue(oldLinefeed)
    }

    def search(parts: Array[String]) = {
        parts.length match {
            case 2 =>
                this.stopHeartbeat()

                val prompt = "Input searching keyword here: ".rendering()
                var regex = readLine(prompt)
                while (regex.toLowerCase != "q") {
                    CliUtils.deleteRowsUpward(1)
                    this.startHeartbeat("searching")
                    val filterList = new FilterList()
                    filterList.addFilter(new RowFilter(CompareOp.EQUAL, new RegexStringComparator(regex)))
                    val iterator = hbaseHandler.scan(parts(1), filterList)
                    this.stopHeartbeat()
                    var input = ""
                    var rowsCount = 0
                    val oldLinefeed = ConfigItems.PRINT_LINEFEED.intValue
                    if (oldLinefeed == 0)
                        ConfigItems.PRINT_LINEFEED.newValue(100)
                    while (iterator.hasNext && input != "q") {
                        rowsCount = iterator.next().prettyShow()
                        input = readLine(Messages.`continue-on-input`).toLowerCase
                        if (ConfigItems.PRINT_COVER.booleanValue)
                            CliUtils.deleteRowsUpward(rowsCount)
                        CliUtils.deleteRowsUpward(1)
                    }
                    regex = readLine(prompt)
                    ConfigItems.PRINT_LINEFEED.newValue(oldLinefeed)
                }
                CliUtils.deleteRowsUpward(1)
        }
    }

    def snapshot(parts: Array[String]) = {
        parts.length match {
            case 3 => hbaseHandler.snapshot(parts(1), parts(2))
        }
        this.stopHeartbeat()
    }

    def invoke(line: String) = {
        val parts = line.split(" ").map(_.trim).filter(_.nonEmpty).map(_.dequote(false))
        if (parts.length > 0) {
            addHistory(line)
            val method = Try(HBaseCommands.getClass.getMethod(parts(0), classOf[Array[String]]))
            if (method.isFailure && method.failed.get.isInstanceOf[NoSuchMethodException]) {
                Messages.`command-not-supported`(parts(0)).prettyPrintln()
            }
            else {
                this.startHeartbeat(parts(0))
                query.forceExit = false
                val r = Try(method.get.invoke(HBaseCommands, parts))
                this.stopHeartbeat()
                if (r.isFailure) {
                    val name = r.failed.get.getClass.getName
                    val cause = r.failed.get.getCause
                    if (name == "scala.MatchError" || (cause != null && cause.getClass.getName == "scala.MatchError"))
                        Messages.`usage`(parts(0)).prettyPrintln()
                    else {
                        ExceptionHandler.handle(r.failed.get)
                    }
                }
            }
        }
    }

}

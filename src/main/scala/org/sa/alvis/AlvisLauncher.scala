package org.sa.alvis

import org.sa.alvis.component.{Alvis, CliDescriptor, ScriptExecutor}
import org.sa.alvis.options.major._
import org.sa.utils.hadoop.hbase.HBaseCatalog
import org.sa.utils.universal.base.Logging
import org.sa.utils.universal.cli.{ParameterOption, PrintConfig}
import org.sa.utils.universal.config.{Config, FileConfig}

/**
 * Created by Stuart Alex on 2017/5/2.
 */
object AlvisLauncher extends App with PrintConfig with Logging {
    override implicit val config: Config = FileConfig()
    private val cli = CliDescriptor.createCli(config, args)
    private val alvis = Alvis()
    this.cli.getOptions.map(o =>
        o.getOpt match {
            case AlterOption.name => 1
            case CompareOption.name => 2
            case GeneratorOption.name => 3
            case LogOption.name => 4
            case ScriptOption.name => 5
            case HelpOption.name => 6
            case QueryOption.name => 7
            case _ => 8
        }).:+(8).min match {
        //Hive Over HBase外部表字段增删
        case 1 => alvis.tableModifier.alter(this.cli)
        //MySQL源表和Hive目标表结构比较
        case 2 => alvis.comparator.compare(this.cli)
        //HBase Catalog生成
        case 3 => HBaseCatalog(cli.getOptionProperties(ParameterOption.name)).display(PRINT_RENDER.stringValue)
        //Spark Application日志查看
        case 4 => alvis.logViewer.display(this.cli)
        //SQL脚本执行
        case 5 => ScriptExecutor(this.cli, alvis, interactive = false).execute()
        //帮助文档输出
        case 6 => CliDescriptor.printHelp()
        //交互式查询器
        case _ => alvis.start()
    }
}
package org.sa.alvis.functions

import org.apache.commons.cli.CommandLine
import org.apache.spark.sql.DataFrame
import org.sa.alvis.component.{Alvis, ExportType}
import org.sa.alvis.options.minor.ExportOption
import org.sa.alvis.options.minor.ExportOption.{MySQLEssentialParameters => MEP, RedisEssentialParameters => REP, SSVEssentialParameters => SEP}
import org.sa.utils.spark.sql.SparkSQL
import org.sa.utils.database.pool.redis.RedisClientPool

/**
 * Created by Stuart Alex on 2017/8/3.
 */
case class Exporter(alvis: Alvis) {

    import alvis._

    /**
     * 将执行器的返回值持久化到指定的组件中
     *
     * @return
     */
    def export(commandLine: CommandLine, data: Object): Unit = {
        val exportType = ExportOption.getOptionValue(commandLine, ExportType.NONE.toString)
        ExportType(exportType) match {
            case ExportType.MySQL => export2MySQL(commandLine, data.asInstanceOf[DataFrame])
            case ExportType.NONE =>
            case ExportType.REDIS => export2Redis(commandLine, data.asInstanceOf[DataFrame])
            case ExportType.SSV => export2SSVFile(commandLine, data.asInstanceOf[DataFrame])
        }
    }

    /**
     * 以MySQL为持久化层
     *
     */
    private def export2MySQL(commandLine: CommandLine, dataFrame: DataFrame): Unit = {
        MEP.validateParameters(config)
        val url = MEP.url.getValue(config)
        val table = MEP.table.getValue(config)
        val mode = MEP.mode.getValue(config)
        SparkSQL.mysql.insert(url, table, dataFrame, mode)
    }

    /**
     * 以Redis为持久化层
     *
     */
    private def export2Redis(commandLine: CommandLine, dataFrame: DataFrame): Unit = {
        REP.validateParameters(config)
        val host = REP.host.getValue(config)
        val port = REP.port.getValue(config)
        val password = REP.password.getValue(config)
        val overwrite = REP.overwrite.getValue(config).toBoolean
        val key = REP.key.getValue(config)
        val id = REP.id.getValue(config).toInt
        val redisClient = RedisClientPool(host, port.toInt, password)
        if (id != 0)
            redisClient.select(id)
        if (overwrite)
            redisClient.del(key)
        val data = dataFrame.toJSON.collect()
        if (data.length > 0)
            redisClient.lpush(key, data: _*)
    }

    /**
     * 以字符分隔文件为持久化层
     *
     */
    private def export2SSVFile(commandLine: CommandLine, dataFrame: DataFrame): Unit = {
        val overwrite = SEP.overwrite.getValue(config)
        val useHeader = SEP.userHeader.getValue(config)
        val delimiter = SEP.delimiter.getValue(config)
        val path = SEP.path.getValue(config)
        if (overwrite.toBoolean)
            fsHandler.delete(path)
        dataFrame.write
            .option("useHeader", useHeader)
            .option("delimiter", delimiter)
            .csv(path)
    }

}

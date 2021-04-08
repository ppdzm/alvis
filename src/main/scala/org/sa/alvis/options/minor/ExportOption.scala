package org.sa.alvis.options.minor

import org.sa.alvis.common.Messages
import org.sa.utils.universal.cli.{CommonOption, CommonParameter}

/**
 * Created by Stuart Alex on 2017/8/4.
 */
object ExportOption extends CommonOption(null, "export") {
    private val hasArg = true

    def option = this.createOption(this.hasArg, this.description).argName(this.argument)

    private def description = Messages.`description-universal-export`(this.MySQLEssentialParameters, this.RedisEssentialParameters, this.SSVEssentialParameters)

    private def argument = Messages.`argument-export`

    object MySQLEssentialParameters extends CommonParameter {
        type MySQLEssentialParameters = Value
        val url = Value("mysql.url")
        val table = Value("mysql.table")
        val mode = Value("data.save.mode")
    }

    object RedisEssentialParameters extends CommonParameter {
        type RedisEssentialParameters = Value
        val host = Value("redis.host")
        val port = Value("redis.port")
        val key = Value("redis.key")
        val password = Value("redis.password")
        val overwrite = Value("redis.overwrite")
        val id = Value("redis.id")

        override def defaultValues = super.defaultValues ++ Map(this.password -> "", this.overwrite -> "false", this.id -> "0")

    }

    object SSVEssentialParameters extends CommonParameter {
        type SSVEssentialParameters = Value
        val overwrite = Value("ssv.overwrite")
        val userHeader = Value("ssv.useHeader")
        val delimiter = Value("ssv.delimiter")
        val path = Value("ssv.path")
    }

}
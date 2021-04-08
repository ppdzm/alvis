package org.sa.alvis.options.major

import org.sa.alvis.common.Messages
import org.sa.utils.universal.cli.{CommonOption, CommonParameter}

/**
 * Created by Stuart Alex on 2017/8/10.
 */
object LogOption extends CommonOption("l", "log") {
    private val hasArg = false

    def option = this.createOption(this.hasArg, this.description)

    private def description = Messages.`description-log`(this.EssentialParameters)

    object EssentialParameters extends CommonParameter {
        type EssentialParameters = Value
        val rm = Value("yarn.rm.address")
        val app = Value("app")
        val role = Value("log.role")
        val logType = Value("log.type")
        val length = Value("log.length")
        val status = Value("executor.status")

        override def defaultValues = super.defaultValues ++ Map(this.logType -> "stderr", this.role -> "driver", this.rm -> null, this.length -> "-4096", this.status -> "active")

    }

}
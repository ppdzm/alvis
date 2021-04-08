package org.sa.alvis.options.minor

import org.sa.alvis.common.Messages
import org.sa.utils.universal.cli.{CommonOption, CommonParameter}

object EngineOption extends CommonOption(null, "engine") {
    private val hasArg = true

    def option = this.createOption(this.hasArg, this.description)

    private def description = Messages.`description-engine`

    object EssentialParameters extends CommonParameter {
        type EssentialParameters = Value
        val url = Value(s"engine.url")
    }

}
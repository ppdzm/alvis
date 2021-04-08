package org.sa.alvis.options.minor

import org.sa.alvis.common.Messages
import org.sa.utils.universal.cli.{CommonOption, CommonParameter}

/**
 * Created by Stuart Alex on 2017/8/7.
 */
object EmailOption extends CommonOption(null, "email") {
    private val hasArg = false

    def option = this.createOption(this.hasArg, this.description)

    private def description = Messages.`description-universal-email`(this.EssentialParameters)

    object EssentialParameters extends CommonParameter {
        type EssentialParameters = Value
        val to = Value("mail.to")
    }

}
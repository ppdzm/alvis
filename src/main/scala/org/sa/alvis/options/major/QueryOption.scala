package org.sa.alvis.options.major

import org.sa.alvis.common.Messages
import org.sa.utils.universal.cli.{CommonOption, CommonParameter}

/**
 * Created by Stuart Alex on 2017/9/4.
 */
object QueryOption extends CommonOption("q", "query") {
    private val hasArg = false

    def option = this.createOption(this.hasArg, this.description)

    private def description = Messages.`description-query`

    object EssentialParameters extends CommonParameter {
        type EssentialParameters = Value
    }

}
package org.sa.alvis.options.major

import org.sa.alvis.common.Messages
import org.sa.utils.universal.cli.CommonOption

/**
 * Created by Stuart Alex on 2017/8/3.
 */
object HelpOption extends CommonOption("h", "help") {
    private val hasArg = false

    def option = this.createOption(this.hasArg, this.description)

    private def description = Messages.`description-help`
}
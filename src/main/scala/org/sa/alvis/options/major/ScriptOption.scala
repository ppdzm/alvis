package org.sa.alvis.options.major

import org.sa.alvis.common.Messages
import org.sa.utils.universal.cli.CommonOption

/**
 * Created by Stuart Alex on 2017/8/4.
 */
object ScriptOption extends CommonOption("s", "script") {
    private val hasArg = true

    def option = this.createOption(this.hasArg, this.description).argName(this.argument)

    private def description = Messages.`description-script`

    private def argument = Messages.`argument-script`
}
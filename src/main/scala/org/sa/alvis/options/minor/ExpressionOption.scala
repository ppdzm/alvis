package org.sa.alvis.options.minor

import org.sa.alvis.common.Messages
import org.sa.utils.universal.cli.CommonOption

/**
 * Created by Stuart Alex on 2017/8/7.
 */
object ExpressionOption extends CommonOption(null, "exp") {
    private val hasArg = true

    def option = this.createOption(this.hasArg, this.description).argName(this.argument)

    private def argument = Messages.`argument-expression`

    private def description = Messages.`description-expression`
}
package org.sa.alvis.component

import org.apache.commons.cli.{CommandLine, Options}
import org.sa.alvis.common.Messages
import org.sa.alvis.options.major._
import org.sa.alvis.options.minor.{EmailOption, EngineOption, ExportOption, ExpressionOption}
import org.sa.utils.universal.cli.CliUtils
import org.sa.utils.universal.config.Config

/**
 * Created by Stuart Alex on 2017/5/2.
 */
object CliDescriptor {
    lazy val programName = "alvis"
    private lazy val registeredLetter = List("a" -> "alter", "c" -> "compare", "e" -> "email", "g" -> "generate",
        "h" -> "help", "l" -> "log", "x" -> "expression",
        "A" -> "add", "B" -> "database", "D" -> "drop", "E" -> "export", "F" -> "file",
        "H" -> "header", "I" -> "index",
        "M" -> "mapping")
    /**
     * 用法语法
     */
    private lazy val usageSyntax = Messages.`help-syntax`(s"${this.programName} [ $AlterOption | $CompareOption | $GeneratorOption | $LogOption | $QueryOption | $ScriptOption | $HelpOption ]")

    def printHelp(): Unit = CliUtils.printHelp(this.usageSyntax, this.header, this.options(), this.footer)

    /**
     * 帮助头
     */
    private def header = Messages.`usage-header`

    /**
     * 帮助脚
     */
    private def footer = Messages.`usage-footer` + s"$AlterOption > $CompareOption > $GeneratorOption > $LogOption > $ScriptOption > $HelpOption > $QueryOption"

    private def options() = {
        new Options()
            .addOption(AlterOption.option)
            .addOptionGroup(AlterOption.optionGroup)
            .addOption(CompareOption.option)
            .addOptionGroup(CompareOption.optionGroup)
            .addOptionGroup(CompareOption.CompareFileOption.optionGroup)
            .addOption(CompareOption.MappingOption.option)
            .addOption(ExpressionOption.option)
            .addOption(EmailOption.option)
            .addOption(EngineOption.option)
            .addOption(ExportOption.option)
            .addOption(GeneratorOption.option)
            .addOption(HelpOption.option)
            .addOption(LogOption.option)
            .addOption(QueryOption.option)
            .addOption(ScriptOption.option)
    }

    def createCli(config: Config, args: Array[String]): CommandLine = {
        val realArgs = ExpressionEvaluator.evaluate(args)
        config.parseOptions(realArgs, this.options())
    }

}

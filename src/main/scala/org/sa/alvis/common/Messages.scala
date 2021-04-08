package org.sa.alvis.common

import org.sa.utils.universal.cli.{CommonOption, CommonParameter, MessageGenerator}

object Messages {

    def `app-index-select`: String = MessageGenerator.generate("app-index-select")

    def `app-log-continue`: String = MessageGenerator.generate("app-log-continue")

    def `app-not-found`(keyword: String): String = MessageGenerator.generate("app-not-found", keyword)

    def `app-not-running`(app: String, status: String): String = MessageGenerator.generate("app-not-running", app, status)

    def `argument-compare-mapping`: String = MessageGenerator.generate("argument-compare-mapping")

    def `argument-export`: String = MessageGenerator.generate("argument-export")

    def `argument-expression`: String = MessageGenerator.generate("argument-expression")

    def `argument-script`: String = MessageGenerator.generate("argument-script")

    def `closed`(url: String): String = MessageGenerator.generate("closed", url)

    def `closing`(index: Integer, url: String): String = MessageGenerator.generate("closing", index, url)

    def `column-address`: String = MessageGenerator.generate("column-address")

    def `column-elapsed-time`: String = MessageGenerator.generate("column-elapsed-time")

    def `column-executor-address`: String = MessageGenerator.generate("column-executor-address")

    def `column-finish-time`: String = MessageGenerator.generate("column-finish-time")

    def `column-is-active`: String = MessageGenerator.generate("column-is-active")

    def `column-launch-time`: String = MessageGenerator.generate("column-launch-time")

    def `column-log-address`: String = MessageGenerator.generate("column-log-address")

    def `column-memory`: String = MessageGenerator.generate("column-memory")

    def `column-name`: String = MessageGenerator.generate("column-name")

    def `column-order-number`: String = MessageGenerator.generate("column-order-number")

    def `column-status`: String = MessageGenerator.generate("column-status")

    def `command-not-supported`(command: String): String = MessageGenerator.generate("command-not-supported", command)

    def `connecting`(url: String): String = MessageGenerator.generate("connecting", url)

    def `continue-on-input`: String = MessageGenerator.generate("continue-on-input")

    def `description-alter`(parameter: CommonParameter): String = MessageGenerator.generate("description-alter", parameter)

    def `description-alter-add`(option: CommonOption): String = MessageGenerator.generate("description-alter-add", option)

    def `description-alter-drop`(option: CommonOption): String = MessageGenerator.generate("description-alter-drop", option)

    def `description-compare`(subOption1: CommonOption, subOption2: CommonOption, parameter: CommonParameter): String = {
        MessageGenerator.generate("description-compare", subOption1, subOption2, parameter)
    }

    def `description-compare-database`(option: CommonOption, parameter: CommonParameter): String = {
        MessageGenerator.generate("description-compare-database", option, parameter)
    }

    def `description-compare-file`(option: CommonOption, subOption1: CommonOption, subOption2: CommonOption): String = {
        MessageGenerator.generate("description-compare-file", option, subOption1, subOption2)
    }

    def `description-compare-file-header`(option: CommonOption, parameter: CommonParameter): String = {
        MessageGenerator.generate("description-compare-file-header", option, parameter)
    }

    def `description-compare-file-index`(option: CommonOption, parameter: CommonParameter): String = {
        MessageGenerator.generate("description-compare-file-index", option, parameter)
    }

    def `description-compare-mapping`(option: CommonOption): String = MessageGenerator.generate("description-compare-mapping", option)

    def `description-engine`: String = MessageGenerator.generate("description-engine")

    def `description-expression`: String = MessageGenerator.generate("description-expression")

    def `description-generator`(parameter: CommonParameter): String = MessageGenerator.generate("description-generator", parameter)

    def `description-help`: String = MessageGenerator.generate("description-help")

    def `description-log`(parameter: CommonParameter): String = MessageGenerator.generate("description-log", parameter)

    def `description-query`: String = MessageGenerator.generate("description-query")

    def `description-script`: String = MessageGenerator.generate("description-script")

    def `description-universal-email`(parameter: CommonParameter): String = MessageGenerator.generate("description-universal-email", parameter)

    def `description-universal-export`(parameters: CommonParameter*): String = MessageGenerator.generate("description-universal-export", parameters: _*)

    def `executor-index-select`: String = MessageGenerator.generate("executor-index-select")

    def `executor-log-continue`: String = MessageGenerator.generate("executor-log-continue")

    def `executor-not-found`: String = MessageGenerator.generate("executor-not-found")

    def `executor-not-in-list`: String = MessageGenerator.generate("executor-not-in-list")

    def `executor-or-driver-log-following`(id: String, role: String, log: String): String = {
        MessageGenerator.generate("executor-or-driver-log-following", id, role, log)
    }

    def `field-not-found-in-json`(field: String, json: String): String = MessageGenerator.generate("field-not-found-in-json", field, json)

    def `filter`(keyword: String): String = MessageGenerator.generate(s"filter-$keyword")

    def `force-exit`: String = MessageGenerator.generate("force-exit")

    def `help`(keyword: String): String = MessageGenerator.generate(s"help-$keyword")

    def `help-syntax`(usage: String): String = MessageGenerator.generate("help-syntax", usage)

    def `invalid-connection`(index: String): String = MessageGenerator.generate("invalid-connection", index)

    def `multiple-matches`(commands: String): String = MessageGenerator.generate("multiple-matches", commands)

    def `no-current-connection`: String = MessageGenerator.generate("no-current-connection")

    def `no-such-method`(method: String, `class`: String): String = MessageGenerator.generate("no-such-method", method, `class`)

    def `parameter-format-limitation`(variable: String, `type`: String): String = MessageGenerator.generate("parameter-format-limitation", variable, `type`)

    def `parameter-value-limitation2`(variable: Object, possibleValues: String*): String = MessageGenerator.generate("parameter-value-limitation2", variable, possibleValues)

    def `parameter-input`(parameter: String): String = MessageGenerator.generate("parameter-input", parameter)

    def `press-again`: String = MessageGenerator.generate("press-again")

    def `query-aborted`: String = MessageGenerator.generate("query-aborted")

    def `reconnecting`(url: String): String = MessageGenerator.generate("reconnecting", url)

    def `row-displayed`(rowCount: Int): String = MessageGenerator.generate("row-displayed", rowCount)

    def `row-effected`(rowCount: Int): String = MessageGenerator.generate("row-effected", rowCount)

    def `row-selected`(rowCount: Int): String = MessageGenerator.generate("row-selected", rowCount)

    def `time-elapsed`(time: String): String = MessageGenerator.generate("time-elapsed", time)

    def `unknown-command`(command: String): String = MessageGenerator.generate("unknown-command", command)

    def `unknown-filter`(filterClass: String): String = MessageGenerator.generate("unknown-filter", filterClass)

    def `unknown-format`(format: String, possibleFormats: String): String = MessageGenerator.generate("unknown-format", format, possibleFormats)

    def `usage`(keyword: String): String = MessageGenerator.generate(s"usage-$keyword")

    def `usage-!get`: String = MessageGenerator.generate("usage-!get")

    def `usage-alter`: String = MessageGenerator.generate("usage-alter")

    def `usage-columns`: String = MessageGenerator.generate("usage-columns")

    def `usage-compare`: String = MessageGenerator.generate("usage-compare")

    def `usage-connect`: String = MessageGenerator.generate("usage-connect")

    def `usage-execute`: String = MessageGenerator.generate("usage-execute")

    def `usage-footer`: String = MessageGenerator.generate("usage-footer")

    def `usage-go`: String = MessageGenerator.generate("usage-go")

    def `usage-header`: String = MessageGenerator.generate("usage-header")

    def `usage-register`: String = MessageGenerator.generate("usage-register")

    def `usage-set`: String = MessageGenerator.generate("usage-set")

}
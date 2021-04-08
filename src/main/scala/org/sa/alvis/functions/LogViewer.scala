package org.sa.alvis.functions

import org.apache.commons.cli.CommandLine
import org.sa.alvis.common.Messages
import org.sa.alvis.component.Alvis
import org.sa.alvis.options.major.LogOption.{EssentialParameters => EP}
import org.sa.utils.hadoop.yarn.{ExecutorState, OutputType, YarnUtils}
import org.sa.utils.universal.base.{DateTimeUtils, SundryUtils}
import org.sa.utils.universal.cli.CliUtils
import org.sa.utils.universal.config.ConfigItems
import org.sa.utils.universal.formats.json.JsonUtils
import org.sa.utils.universal.implicits.BasicConversions._
import org.sa.utils.universal.implicits.SeqLikeConversions._
import org.sa.utils.universal.implicits.UnitConversions._

import scala.util.Try
import scala.util.matching.Regex

/**
 * Created by Stuart Alex on 2017/8/10.
 */
case class LogViewer(alvis: Alvis) {

    import alvis._

    private lazy val resourceManager = RESOURCE_MANAGER_ADDRESS.arrayValue()

    /**
     * 显示日志
     *
     * @param commandLine CommandLine
     */
    def display(commandLine: CommandLine): Unit = {
        EP.validateParameters(config)
        val role = EP.role.getValue(config)
        if (role.toLowerCase != "driver" && role.toLowerCase != "executor")
            throw new IllegalArgumentException(Messages.`parameter-value-limitation2`(EP.role, "driver", "executor"))
        val app = EP.app.getValue(config)
        if (app.matches("""application_\d+_\d+""")) {
            val json = YarnUtils.sparkAppJson(app, resourceManager)
            this.viewViaJson(json)
        }
        else {
            val apps = {
                val temp = YarnUtils.sparkApps(app, resourceManager)
                if (temp.exists(_.get("state").asText.toLowerCase == "running"))
                    temp.filter(_.get("state").asText.toLowerCase == "running").zipWithIndex
                else
                    temp.zipWithIndex
            }
            apps.length match {
                case 0 =>
                    Messages.`app-not-found`(app).prettyPrintln()
                    this.exit()
                case 1 =>
                    this.viewViaJson(apps.head._1.toString)
                case _ =>
                    var exit = false
                    val columns = List(
                        Messages.`column-order-number`,
                        "application id",
                        Messages.`column-name`,
                        Messages.`column-status`,
                        Messages.`column-memory`,
                        Messages.`column-launch-time`,
                        Messages.`column-finish-time`,
                        Messages.`column-elapsed-time`)
                    while (!exit) {
                        apps.map(e => {
                            val number = e._2 + 1
                            val id = e._1.get("id").asText
                            val name = e._1.get("name").asText
                            val state = e._1.get("state").asText
                            val memory = (e._1.get("allocatedMB").asLong() * 1024 * 1024).toBytesLength
                            val startedTime = DateTimeUtils.format(e._1.get("startedTime").asLong(), "yyyy-MM-dd HH:mm:ss")
                            val elapsedTime = (e._1.get("elapsedTime").asLong() / 1000).longTimeFormat(ConfigItems.LANGUAGE.stringValue)
                            val finishedTime =
                                if (e._1.get("finishedTime").asLong() == 0)
                                    ""
                                else
                                    DateTimeUtils.format(e._1.get("finishedTime").asLong(), "yyyy-MM-dd HH:mm:ss")
                            List(number, id, name, state, memory, startedTime, finishedTime, elapsedTime)
                        }).prettyShow(columns = columns)
                        var input = readLine(Messages.`app-index-select`)
                        while (input != "q" && !apps.exists(_._1.get("id").asText == input) && !apps.exists(e => (e._2 + 1).toString == input))
                            input = readLine(Messages.`app-index-select`)
                        if (input != "q") {
                            val json = apps.find(_._1.get("id").asText == input).orElse(apps.find(e => (e._2 + 1).toString == input)).get._1.toString
                            this.viewViaJson(json)
                            val q = readLine(Messages.`app-log-continue`).toLowerCase
                            CliUtils.deleteRowsUpward(1)
                            if (q == "q")
                                exit = true
                        } else {
                            exit = true
                        }
                    }
                    this.exit()
            }
        }
    }

    /**
     * 通过App详情json查看日志
     *
     * @param json App详情
     */
    private def viewViaJson(json: String): Unit = {
        val app = EP.app.getValue(config)
        val role = EP.role.getValue(config)
        role.toLowerCase match {
            case "driver" => this.displayDriverLog(json)
            case "executor" =>
                val state = new Regex(""""state":"(.*?)"""", "state").findFirstMatchIn(json)
                    .getOrElse(throw new NoSuchFieldException(Messages.`field-not-found-in-json`("state", json))).group("state").toLowerCase
                if (state != "running") {
                    Messages.`app-not-running`(app, state).prettyPrintln()
                } else
                    this.displayExecutorLog(json)
        }
    }

    /**
     * 显示指定Yarn Application的Driver日志
     *
     * @param json Yarn Application详情JSON
     */
    private def displayDriverLog(json: String): Unit = {
        val id = new Regex(""""id":"(.*?)"""", "id").findFirstMatchIn(json)
            .getOrElse(throw new NoSuchFieldException(Messages.`field-not-found-in-json`("id", JsonUtils.pretty(json)))).group("id")
        val log = YarnUtils.driverLogs(id, EP.length.getValue(config), OutputType.withName(EP.logType.getValue(config)), resourceManager)
        Messages.`executor-or-driver-log-following`(id, "Driver", log).prettyPrintln()
    }

    /**
     * 交互式查看或持久化Yarn Application的Executor日志
     *
     * @param json Yarn Application详情JSON
     */
    private def displayExecutorLog(json: String): Unit = {
        //    val jsonNode = JsonUtils.parse(json)
        //    val id = Option(jsonNode.get("id")).getOrElse(jsonNode.get("app").get("id")).asText
        val id = new Regex(""""id":"(.*?)"""", "id").findFirstMatchIn(json)
            .getOrElse(throw new NoSuchFieldException(Messages.`field-not-found-in-json`("id", JsonUtils.pretty(json))))
            .group("id")
        val logType = EP.logType.getValue(config)
        var exit = false
        while (!exit) {
            val (columns, executors) = YarnUtils.executorsV2(id, ExecutorState.withName(EP.status.getValue(config)), resourceManager)
            if (executors.isEmpty) {
                val input = readLine(Messages.`executor-not-found`)
                if (input == "Y" || input == "y")
                    SundryUtils.waiting(null, 60)
                else
                    exit = true
            } else {
                executors.prettyShow(columns = columns)
                val branches = Array("q", "s")
                var inputs = readLine(Messages.`executor-index-select`).split(" ").filter(_.length > 0)
                var legal = false
                while (!legal) {
                    if (inputs.length == 0)
                        inputs = readLine(Messages.`executor-index-select`).split(" ").filter(_.length > 0)
                    else if (!branches.contains(inputs(0))) {
                        if (Try(inputs(0).toInt).isFailure)
                            inputs = readLine(Messages.`parameter-format-limitation`("Number", "numeric") + Messages.`executor-index-select`).split(" ").filter(_.length > 0)
                        else if (Try(if (inputs.length > 1) inputs(1).toInt else -4096).isFailure)
                            inputs = readLine(Messages.`parameter-format-limitation`("Length", "numeric") + Messages.`executor-index-select`).split(" ").filter(_.length > 0)
                        else if (executors.forall(_.head != inputs(0)))
                            inputs = readLine(Messages.`executor-not-in-list` + Messages.`executor-index-select`).split(" ").filter(_.length > 0)
                        else
                            legal = true
                    } else
                        legal = true
                }
                val number = inputs(0)
                number match {
                    case "q" => exit = true
                    case "s" =>
                        YarnUtils.saveExecutorsPage(id, resourceManager)
                        exit = true
                    case _ =>
                        val length = if (inputs.length > 1) inputs(1) else "-4096"
                        val address = executors.find(_.head == number).get(3)
                        val log = YarnUtils.executorLogs(address, length, OutputType.withName(logType))
                        Messages.`executor-or-driver-log-following`(id, s"Executor $number", log).prettyPrintln()
                        val q = readLine(Messages.`executor-log-continue`).toLowerCase
                        CliUtils.deleteRowsUpward(1)
                        if (q == "q")
                            exit = true
                }
            }
        }
    }

    private def exit(): Unit = {
        if (!IS_INTERACTIVE.booleanValue)
            sys.exit()
    }

}

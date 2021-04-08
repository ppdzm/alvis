package org.sa.alvis.common

import org.sa.utils.universal.config.{ConfigItem, ConfigTrait}
import org.sa.utils.universal.core.CoreConstants._

/**
 * @author StuartAlex on 2019-07-30 15:04
 */
trait AlvisConfigConstants extends ConfigTrait {
    lazy val HISTORY_FILE_PATH: String = ConfigItem("historyFile.path", ConfigItem(profilePathKey).stringValue).stringValue + s"/.history"
    lazy val CONSOLE_EXIT: ConfigItem = ConfigItem("console.exit", false)
    lazy val CONSOLE_SILENT: ConfigItem = ConfigItem("console.silent", false)
    lazy val CONNECTION_ON_START: ConfigItem = ConfigItem("connection.onStart")
    lazy val IS_INTERACTIVE: ConfigItem = ConfigItem("isInteractive", false)
    lazy val IGNORE_EXCEPTION: ConfigItem = ConfigItem("exception.ignore", true)

    def engineUrl(engine: String): ConfigItem = ConfigItem(s"engine.$engine.url")

}

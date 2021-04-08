package org.sa.alvis.component

import org.sa.alvis.common.Messages
import org.sa.utils.universal.base.Mathematics
import org.sa.utils.universal.implicits.BasicConversions._

object TinyUtils {

    def timeCost(start: Long, end: Long, count: (Int, Int)): Unit = {
        val selected = Messages.`row-selected`(count._1)
        val displayed = Messages.`row-displayed`(count._2)
        val elapsed = Messages.`time-elapsed`(Mathematics.floor((end - start) / 1000d, 3).toString.trimEnd("0").trimEnd("."))
        (selected + ", " + displayed + elapsed).prettyPrintln()
    }
}

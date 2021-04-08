package org.sa.alvis.component

import org.sa.utils.universal.base.Enum

/**
 * Created by Stuart Alex on 2017/10/23.
 */
object ExportType extends Enum {
    type ExportType = Value
    final val MySQL = Value("mysql")
    final val REDIS = Value("redis")
    final val SSV = Value("ssv")
    final val NONE = Value("none")
}

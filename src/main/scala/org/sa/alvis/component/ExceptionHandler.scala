package org.sa.alvis.component

import org.sa.utils.universal.implicits.BasicConversions._

/**
 * Created by Stuart Alex on 2017/9/7.
 */
object ExceptionHandler {

    def handle(throwable: Throwable): Unit = {
        throw throwable
//        throwable.printStackTrace()
//        if (throwable.getMessage.isNullOrEmpty) {
//            if (throwable.getCause.notNull)
//                handle(throwable.getCause)
//        } else {
//            (throwable.getClass.getName + ":" + throwable.getMessage).prettyPrintln()
//        }
    }

}

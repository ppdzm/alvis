package org.sa.alvis.component

import org.sa.alvis.options.minor.ExpressionOption
import org.sa.utils.universal.cli.ParameterOption
import org.sa.utils.universal.feature.{Compiler, ExceptionGenerator}
import org.sa.utils.universal.implicits.BasicConversions._

import scala.collection.mutable.ListBuffer

/**
 * Created by Stuart Alex on 2017/8/8.
 */
object ExpressionEvaluator {
    private lazy val compiler = Compiler(None)

    /**
     * 动态解析主程序参数中的表达式并求值
     *
     * @param args 主程序参数
     * @return
     */
    def evaluate(args: Array[String]): Array[String] = {
        val buffer = ListBuffer[String]()
        var i = 0
        while (i < args.length) {
            args(i).trimStart("-") match {
                case ExpressionOption.longName =>
                    if (args.length <= i + 1)
                        throw ExceptionGenerator.newException("PropertyNotPerfect", s"Last argument of $ExpressionOption option is not provided")
                    else {
                        buffer += "-" + ParameterOption.name
                        val ve = args(i + 1).split("=")
                        if (ve.length != 2)
                            throw new IllegalArgumentException(s"Format of argument for $ExpressionOption option is not <name=value>")
                        buffer += ve(0) + "=" + this.compiler.evaluate(ve(1))
                        i += 2
                    }
                case _ =>
                    buffer += args(i)
                    i += 1
            }
        }
        buffer.toArray
    }

}

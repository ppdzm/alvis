package org.sa.alvis.common

import org.sa.alvis.format._
import org.sa.utils.universal.base.Symbols
import org.sa.utils.universal.base.Symbols.{comma, pipeOperator, tab}

/**
 * @author StuartAlex on 2019-07-30 15:04
 */
object AlvisConstants {
    lazy val programName = "alvis"
    lazy val COMMAND_PREFIX: String = Symbols.exclamationMark
    lazy val REFERENCE_PREFIX: String = Symbols.hashKey
    lazy val PARAMETER_PREFIX: String = Symbols.dollar
    lazy val REGISTERED_FORMATS = Map("csv" -> SeparatedValuesOutputFormat(comma),
        "default" -> DefaultOutputFormat,
        "dsv" -> SeparatedValuesOutputFormat(pipeOperator),
        "json" -> JsonOutputFormat,
        "tsv" -> SeparatedValuesOutputFormat(tab),
        "vertical" -> VerticalOutputFormat,
        "xml_attributes" -> XMLAttributesOutputFormat,
        "xml_elements" -> XMLElementsOutputFormat)
}

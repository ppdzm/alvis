package org.sa.alvis.format

import java.sql.ResultSet

import org.apache.spark.sql.DataFrame
import org.sa.utils.spark.implicits.DataFrameConversions._
import org.sa.utils.universal.implicits.ResultSetConversions._
import org.sa.utils.universal.implicits.SeqLikeConversions._

/**
 * Created by Stuart Alex on 2017/9/12.
 */
object XMLAttributesOutputFormat extends OutputFormat {

    override def format[T](seq: Seq[Seq[T]], columns: Seq[String]) = seq.toXMLWithAttributes("List", "Row", columns)

    override def format(resultSet: ResultSet) = resultSet.toXMLWithAttributes("ResultSet", "Row")

    override def format(dataFrame: DataFrame) = dataFrame.toXMLWithAttributes("DataFrame", "Row")

}
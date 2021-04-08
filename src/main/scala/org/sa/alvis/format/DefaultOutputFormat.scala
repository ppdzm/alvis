package org.sa.alvis.format

import java.sql.ResultSet

import org.apache.spark.sql.DataFrame
import org.sa.utils.spark.implicits.DataFrameConversions._
import org.sa.utils.universal.implicits.ResultSetConversions._
import org.sa.utils.universal.implicits.SeqLikeConversions._

/**
 * Created by Stuart Alex on 2017/9/13.
 */
object DefaultOutputFormat extends OutputFormat {

    override def format[T](seq: Seq[Seq[T]], columns: Seq[String]): (String, Seq[String], String) = seq.toTable(columns = columns)

    override def format(resultSet: ResultSet): (String, Seq[String], String) = resultSet.toTable()

    override def format(dataFrame: DataFrame): (String, Seq[String], String) = dataFrame.toTable()

}
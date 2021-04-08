package org.sa.alvis.format

import java.sql.ResultSet

import org.apache.spark.sql.DataFrame
import org.sa.utils.spark.implicits.DataFrameConversions._
import org.sa.utils.universal.implicits.ResultSetConversions._

/**
 * Created by Stuart Alex on 2017/9/12.
 */
case class SeparatedValuesOutputFormat(separator: String) extends OutputFormat {

    override def format[T](seq: Seq[Seq[T]], columns: Seq[String]): (String, Seq[String], String) = {
        (columns.mkString(this.separator), seq.map(_.mkString(this.separator)), "")
    }

    override def format(resultSet: ResultSet): (String, List[String], String) = {
        (resultSet.columns.mkString(this.separator), resultSet.toList.map(_.mkString(this.separator)), "")
    }

    override def format(dataFrame: DataFrame): (String, List[String], String) = {
        (dataFrame.columns.mkString(this.separator), dataFrame.toArray.map(_.mkString(this.separator)).toList, "")
    }

}
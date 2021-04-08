package org.sa.alvis.format

import java.sql.ResultSet

import org.apache.spark.sql.DataFrame

/**
 * Created by Stuart Alex on 2017/9/12.
 */
trait OutputFormat {

    def format[T](seq: Seq[T], column: String): (String, Seq[String], String) = this.format(seq.map(Seq(_)), Seq(column))

    def format[T](array: Array[T], column: String): (String, Seq[String], String) = this.format(array.map(Seq(_)), Seq(column))

    def format[T](seq: Seq[Seq[T]], columns: Seq[String]): (String, Seq[String], String)

    def format(resultSet: ResultSet): (String, Seq[String], String)

    def format(dataFrame: DataFrame): (String, Seq[String], String)

}
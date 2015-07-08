/**
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.stratio.sparkta.testat


import com.stratio.sparkta.sdk.DateOperations
import org.apache.spark.SparkContext
import org.apache.spark.sql.{Row, SQLContext}

import scala.reflect.io.File


/**
 * Acceptance test:
 *   [Input]: Socket.
 *   [Output]: MongoDB.
 *   [Operators]: sum, avg.
 * @author gschiavon
 */
class ISocketOCsvOperatorsIT extends SparktaATSuite {

  override val PolicyEndSleep = 30000
  override val policyFile = "policies/ISocket-OCsv.json"
  override val PathToCsv = getClass.getClassLoader.getResource("fixtures/at-data-operators.csv").getPath
  val csvOutputPath = policyDto.outputs(0).configuration("path").toString
  val NumExecutors = 4
  val NumEventsExpected : String= "8"
  "Sparkta" should {
    "starts and executes a policy that reads from a socket and writes in csv" in {
      sparktaRunner
      checkCsvData(csvOutputPath)
    }


    def checkCsvData(path : String): Unit ={
      val pathProductTimestamp = path + s"product_timestamp${DateOperations.subPath("day", None)}.csv"
      val sqlContext = new SQLContext(new SparkContext(s"local[$NumExecutors]", "ISocketOCsv"))
      val df = sqlContext.read.format("com.databricks.spark.csv").option("header", "true").load(pathProductTimestamp)

      df.count should be (2)
      df.collect.foreach(row => {
        row.getAs[String]("product").toString match {
          case "producta" => {
            val aValues = extractProductValues(row)

            aValues("avg") should be("639.0")
            aValues("sum") should be("5112.0")
            aValues("count") should be(NumEventsExpected)
            aValues("first") should be("10")
            aValues("last") should be("600")
            aValues("max") should be("1002.0")
            aValues("min") should be("10.0")
            aValues("fulltext") should be("10 500 1000 500 1000 500 1002 600")
            aValues("stddev") should be("347.9605889013459")
            aValues("variance") should be("121076.57142857143")
            aValues("range") should be("992.0")
          }
          case "productb" => {
            val bValues = extractProductValues(row)

            bValues("avg") should be("758.25")
            bValues("sum") should be("6066.0")
            bValues("count")  should be(NumEventsExpected)
            bValues("first") should be("15")
            bValues("last") should be("50")
            bValues("max") should be("1001.0")
            bValues("min") should be("15.0")
            bValues("fulltext") should be("15 1000 1000 1000 1000 1000 1001 50")
            bValues("stddev")  should be("448.04041590655")
            bValues("variance") should be("200740.2142857143")
            bValues("range") should be("986.0")
          }
        }
      })
    }

    def extractProductValues(row : Row) : Map[String, String] = Map(
      "avg" -> row.getAs[String]("avg_price"),
      "sum" -> row.getAs[String]("sum_price"),
      "count" -> row.getAs[String]("count_price"),
      "first" -> row.getAs[String]("first_price"),
      "last" -> row.getAs[String]("last_price"),
      "max" -> row.getAs[String]("max_price"),
      "min" -> row.getAs[String]("min_price"),
      "fulltext" -> row.getAs[String]("fulltext_price"),
      "stddev" -> row.getAs[String]("stddev_price"),
      "variance" -> row.getAs[String]("variance_price"),
      "range" -> row.getAs[String]("range_price"))

  }



  override def extraAfter: Unit = File(csvOutputPath).deleteRecursively

  override def extraBefore: Unit = {}

}
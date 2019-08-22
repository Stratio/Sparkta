/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.json

import java.io.{Serializable => JSerializable}

import com.stratio.sparta.plugin.TemporalSparkContext
import com.stratio.sparta.core.DistributedMonad.DistributedMonadImplicits
import com.stratio.sparta.core.models.{OutputOptions, OutputWriterOptions, TransformationStepManagement}
import com.stratio.sparta.core.enumerators.SaveModeEnum
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class JsonPathTransformStepBatchIT extends TemporalSparkContext with Matchers with DistributedMonadImplicits {

  "A JsonTransformStepBatchIT" should "transform json events the input Dataset" in {
    val JSON =
      """{ "store": {
        |    "book": [
        |      { "category": "reference",
        |        "author": "Nigel Rees",
        |        "title": "Sayings of the Century",
        |        "price": 8.95
        |      }
        |    ],
        |    "bicycle": {
        |      "color": "red",
        |      "price": 19.95
        |    }
        |  }
        |}""".stripMargin
    val queries =
      """[
        |{
        |   "field":"color",
        |   "query":"$.store.bicycle.color",
        |   "type":"string"
        |},
        |{
        |   "field":"price",
        |   "query":"$.store.bicycle.price",
        |   "type":"double"
        |}]
        | """.stripMargin

    val inputField = "json"
    val inputSchema = StructType(Seq(StructField(inputField, StringType)))
    val outputSchema = StructType(Seq(StructField("color", StringType), StructField("price", DoubleType)))
    val dataIn = Seq(new GenericRowWithSchema(Array(JSON), inputSchema))
    val dataInRow = dataIn.map(_.asInstanceOf[Row])
    val dataOut = Seq(new GenericRowWithSchema(Array("red", 19.95), outputSchema))
    val dataSet = sc.parallelize(dataInRow)
    val inputData = Map("step1" -> dataSet)
    val outputOptions = OutputWriterOptions.defaultOutputOptions("stepName", None, Option("tableName"))

    val result = new JsonPathTransformStepBatch(
      "dummy",
      outputOptions,
      TransformationStepManagement(),
      Option(ssc),
      sparkSession,
      Map("queries" -> queries.asInstanceOf[JSerializable],
        "inputField" -> inputField,
        "fieldsPreservationPolicy" -> "REPLACE")
    ).transformWithDiscards(inputData)._1

    val arrayValues = result.ds.collect()

    arrayValues.foreach { row =>
          assert(dataOut.contains(row))
          assert(outputSchema == row.schema)
        }

    assert(arrayValues.length === 1)
  }

  "A JsonTransformStepBatchIT" should "discard rows" in {
    val JSON =
      """{ "store": {
        |    "book": [
        |      { "category": "reference",
        |        "author": "Nigel Rees",
        |        "title": "Sayings of the Century",
        |        "price": 8.95
        |      }
        |    ],
        |    "bicycle": {
        |      "color": "red",
        |      "price": 19.95
        |    }
        |  }
        |}""".stripMargin
    val queries =
      """[
        |{
        |   "field":"color",
        |   "query":"$.store.bicycle.color",
        |   "type":"string"
        |},
        |{
        |   "field":"price",
        |   "query":"$.store.bicycle.price",
        |   "type":"double"
        |}]
        | """.stripMargin

    val inputField = "json"
    val inputSchema = StructType(Seq(StructField(inputField, StringType)))
    val outputSchema = StructType(Seq(StructField("color", StringType), StructField("price", DoubleType)))
    val dataIn = Seq(
      new GenericRowWithSchema(Array(JSON), inputSchema),
      new GenericRowWithSchema(Array("wrong data"), inputSchema)
    )
    val dataInRow = dataIn.map(_.asInstanceOf[Row])
    val dataOut = Seq(new GenericRowWithSchema(Array("red", 19.95), outputSchema))
    val dataSet = sc.parallelize(dataInRow)
    val inputData = Map("step1" -> dataSet)
    val outputOptions = OutputWriterOptions.defaultOutputOptions("stepName", None, Option("tableName"))

    val result = new JsonPathTransformStepBatch(
      "dummy",
      outputOptions,
      TransformationStepManagement(),
      Option(ssc),
      sparkSession,
      Map("queries" -> queries.asInstanceOf[JSerializable],
        "inputField" -> inputField,
        "whenRowError" -> "RowDiscard",
        "fieldsPreservationPolicy" -> "REPLACE")
    ).transformWithDiscards(inputData)

    val validData = result._1.ds.collect()
    val discardedData = result._3.get.ds.collect()

    validData.foreach { row =>
      assert(dataOut.contains(row))
      assert(outputSchema == row.schema)
    }

    discardedData.foreach { row =>
      assert(dataIn.contains(row))
      assert(inputSchema == row.schema)
    }

    assert(validData.length === 1)
    assert(discardedData.length === 1)
  }
}
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

package com.stratio.sparkta.aggregator

import java.io.{Serializable => JSerializable}

import com.stratio.sparkta.plugin.dimension.default._
import org.apache.spark.streaming.TestSuiteBase
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.stratio.sparkta.plugin.operator.count.CountOperator
import com.stratio.sparkta.plugin.operator.sum.SumOperator
import com.stratio.sparkta.sdk._

@RunWith(classOf[JUnitRunner])
class CubeSpec extends TestSuiteBase {

  test("aggregate") {

    val PreserverOrder = true
    val nativeDimension = new DefaultDimension
    val checkpointInterval = 10000
    val checkpointTimeAvailability = 60000
    val checkpointGranularity = "minute"
    val eventGranularity = DateOperations.dateFromGranularity(DateTime.now(), "minute")
    val name = "cubeName"
    val outputs = Seq()
    val multiplexer = false
    val cube = new Cube(
      name,
      Seq(DimensionPrecision(Dimension("foo", nativeDimension), new Precision("identity", TypeOp.String))),
      Seq(new CountOperator(Map()), new SumOperator(Map("inputField" -> "n"))),
      outputs,
      multiplexer,
      checkpointInterval,
      checkpointGranularity,
      checkpointTimeAvailability)

    testOperation(getInput, cube.aggregate, getOutput, PreserverOrder)

    def getInput: Seq[Seq[(DimensionValuesTime, Map[String, JSerializable])]] = Seq(Seq(
      (DimensionValuesTime(Seq(DimensionValue(DimensionPrecision(Dimension("foo", nativeDimension),
        new Precision("identity", TypeOp.String)), "bar")), eventGranularity), Map[String, JSerializable]("n" -> 4)),
      (DimensionValuesTime(Seq(DimensionValue(DimensionPrecision(Dimension("foo", nativeDimension),
        new Precision("identity", TypeOp.String)), "bar")), eventGranularity), Map[String, JSerializable]("n" -> 3)),
      (DimensionValuesTime(Seq(DimensionValue(DimensionPrecision(Dimension("foo", nativeDimension),
        new Precision("identity", TypeOp.String)), "foo")), eventGranularity), Map[String, JSerializable]("n" -> 3))),
      Seq(
        (DimensionValuesTime(Seq(DimensionValue(DimensionPrecision(Dimension("foo", nativeDimension),
          new Precision("identity", TypeOp.String)), "bar")), eventGranularity), Map[String, JSerializable]("n" -> 4)),
        (DimensionValuesTime(Seq(DimensionValue(DimensionPrecision(Dimension("foo", nativeDimension),
          new Precision("identity", TypeOp.String)), "bar")), eventGranularity), Map[String, JSerializable]("n" -> 3)),
        (DimensionValuesTime(Seq(DimensionValue(DimensionPrecision(Dimension("foo", nativeDimension),
          new Precision("identity", TypeOp.String)), "foo")), eventGranularity), Map[String, JSerializable]("n" -> 3))))

    def getOutput: Seq[Seq[(DimensionValuesTime, Map[String, Option[Any]])]] = Seq(
      Seq(
        (DimensionValuesTime(Seq(DimensionValue(DimensionPrecision(Dimension("foo", new DefaultDimension),
          new Precision("identity", TypeOp.String)), "bar")), eventGranularity),
          Map("count" -> Some(2L), "sum_n" -> Some(7L))),
        (DimensionValuesTime(Seq(DimensionValue(DimensionPrecision(Dimension("foo", new DefaultDimension),
          new Precision("identity", TypeOp.String)), "foo")), eventGranularity),
          Map("count" -> Some(1L), "sum_n" -> Some(3L)))),
      Seq(
        (DimensionValuesTime(Seq(DimensionValue(DimensionPrecision(Dimension("foo", new DefaultDimension),
          new Precision("identity", TypeOp.String)), "bar")), eventGranularity),
          Map("count" -> Some(4L), "sum_n" -> Some(14L))),
        (DimensionValuesTime(Seq(DimensionValue(DimensionPrecision(Dimension("foo", new DefaultDimension),
          new Precision("identity", TypeOp.String)), "foo")), eventGranularity),
          Map("count" -> Some(2L), "sum_n" -> Some(6L)))))
  }
}

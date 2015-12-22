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

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparkta.sdk._
import com.stratio.sparkta.serving.core.SparktaConfig
import com.stratio.sparkta.serving.core.constants.AppConstant
import org.apache.spark.HashPartitioner
import org.apache.spark.streaming.Duration
import org.apache.spark.streaming.dstream.DStream
import org.joda.time.DateTime

import scala.util.Try

/**
 * Use this class to describe a cube that you want the multicube to keep.
 *
 * For example, if you're counting events with the dimensions (color, size, flavor) and you
 * want to keep a total count for all (color, size) combinations, you'd specify that using a Cube
 */

case class Cube(name: String,
                dimensions: Seq[Dimension],
                operators: Seq[Operator],
                checkpointTimeDimension: String,
                checkpointInterval: Int,
                checkpointGranularity: String,
                checkpointTimeAvailability: Long) extends SLF4JLogging {

  private val associativeOperators = operators.filter(op => op.isAssociative)
  private lazy val associativeOperatorsMap = associativeOperators.map(op => op.key -> op).toMap
  private val nonAssociativeOperators = operators.filter(op => !op.isAssociative)
  private lazy val nonAssociativeOperatorsMap = nonAssociativeOperators.map(op => op.key -> op).toMap
  private lazy val rememberPartitioner =
    Try(SparktaConfig.getDetailConfig.get.getBoolean(AppConstant.ConfigRememberPartitioner))
      .getOrElse(AppConstant.DefaultRememberPartitioner)
  private final val NotUpdatedAggregations = 0
  private final val UpdatedAggregations = 1

  /**
   * Aggregation process that have 4 ways:
   * 1. Cube with associative operators only.
   * 2. Cube with non associative operators only.
   * 3. Cube with associtaive and non associative operators.
   * 4. Cube with no operators.
   */

  def aggregate(dimensionsValues: DStream[(DimensionValuesTime,
    Map[String, JSerializable])]): DStream[(DimensionValuesTime, Map[String, Option[Any]])] = {

    val filteredValues = filterDimensionValues(dimensionsValues)
    val associativesCalculated = if (associativeOperators.nonEmpty)
      Option(updateAssociativeState(associativeAggregation(filteredValues)))
    else None
    val nonAssociativesCalculated = if (nonAssociativeOperators.nonEmpty)
      Option(aggregateNonAssociativeValues(updateNonAssociativeState(filteredValues)))
    else None

    (associativesCalculated, nonAssociativesCalculated) match {
      case (Some(associativeValues), Some(nonAssociativeValues)) =>
        associativeValues.cogroup(nonAssociativeValues)
          .mapValues { case (associativeAggregations, nonAssociativeAggregations) =>
            (associativeAggregations.flatten ++ nonAssociativeAggregations.flatten).toMap
          }
      case (Some(associativeValues), None) => associativeValues
      case (None, Some(nonAssociativeValues)) => nonAssociativeValues
      case _ =>
        log.warn("You should define operators for aggregate input values")
        noAggregationsState(dimensionsValues)
    }
  }

  /**
   * Filter dimension values that correspond with the current cube dimensions
   */

  protected def filterDimensionValues(dimensionValues: DStream[(DimensionValuesTime, Map[String, JSerializable])])
  : DStream[(DimensionValuesTime, (Map[String, JSerializable], Int))] = {

    dimensionValues.map { case (dimensionsValuesTime, aggregationValues) =>
      val dimensionsFiltered = dimensionsValuesTime.dimensionValues.filter(dimVal =>
        dimensions.exists(comp => comp.name == dimVal.dimension.name))

      (DimensionValuesTime(dimensionsFiltered, dimensionsValuesTime.time, checkpointTimeDimension),
        (aggregationValues, UpdatedAggregations))
    }
  }

  protected def updateNonAssociativeState(dimensionsValues: DStream[(DimensionValuesTime,
    (Map[String, JSerializable], Int))]): DStream[(DimensionValuesTime, Seq[(String, Option[Any])])] = {

    dimensionsValues.checkpoint(new Duration(checkpointInterval))

    val newUpdateFunc = (iterator: Iterator[(DimensionValuesTime,
      Seq[(Map[String, JSerializable], Int)],
      Option[(Seq[(String, Option[Any])], Int)])]) => {

      val eventTime =
        DateOperations.dateFromGranularity(DateTime.now(), checkpointGranularity) - checkpointTimeAvailability

      iterator.filter(dimensionsData => dimensionsData._1.time >= eventTime)
        .flatMap { case (dimensionsKey, values, state) =>
          updateNonAssociativeFunction(values, state).map(result => (dimensionsKey, result))
        }
    }
    val valuesCheckpointed = dimensionsValues.updateStateByKey(
      newUpdateFunc, new HashPartitioner(dimensionsValues.context.sparkContext.defaultParallelism), rememberPartitioner)

    filterUpdatedAggregations(valuesCheckpointed)
  }

  protected def updateNonAssociativeFunction(values: Seq[(Map[String, JSerializable], Int)],
                                             state: Option[(Seq[(String, Option[Any])], Int)])
  : Option[(Seq[(String, Option[Any])], Int)] = {

    val proccessMapValues = values.flatMap { case (inputFields, isNewValue) =>
      nonAssociativeOperators.map(op => op.processMap(inputFields) match {
        case Some(values) => op.key -> Some(values)
        case None => op.key -> None
      })
    }
    val lastState = state match {
      case Some(checkpointState) => checkpointState._1
      case None => Seq()
    }

    getUpdatedAggregations(lastState ++ proccessMapValues, values.isEmpty)
  }

  protected def aggregateNonAssociativeValues(dimensionsValues: DStream[(DimensionValuesTime,
    Seq[(String, Option[Any])])])
  : DStream[(DimensionValuesTime, Map[String, Option[Any]])] =

    dimensionsValues.mapValues(aggregationValues => {
      aggregationValues.groupBy { case (key, value) => key }
        .map { case (name, value) =>
          (name, nonAssociativeOperatorsMap(name).processReduce(value.map { case (opKey, opValue) => opValue }))
        }
    })

  protected def updateAssociativeState(dimensionsValues: DStream[(DimensionValuesTime,
    (Seq[(String, Option[Any])], Int))]):
  DStream[(DimensionValuesTime, Map[String, Option[Any]])] = {

    dimensionsValues.checkpoint(new Duration(checkpointInterval))

    val newUpdateFunc = (iterator: Iterator[(DimensionValuesTime,
      Seq[(Seq[(String, Option[Any])], Int)],
      Option[(Map[String, Option[Any]], Int)])]) => {

      val eventTime =
        DateOperations.dateFromGranularity(DateTime.now(), checkpointGranularity) - checkpointTimeAvailability

      iterator.filter(dimensionsData => dimensionsData._1.time >= eventTime)
        .flatMap { case (dimensionsKey, values, state) =>
          updateAssociativeFunction(values, state).map(result => (dimensionsKey, result))
        }
    }

    val valuesCheckpointed = dimensionsValues.updateStateByKey(
      newUpdateFunc, new HashPartitioner(dimensionsValues.context.sparkContext.defaultParallelism), rememberPartitioner)


    filterUpdatedAggregations(valuesCheckpointed)
  }

  def associativeAggregation(dimensionsValues: DStream[(DimensionValuesTime, (Map[String, JSerializable], Int))]):
  DStream[(DimensionValuesTime, (Seq[(String, Option[Any])], Int))] =
    dimensionsValues.mapValues { case (inputFields, isNewValue) =>
      associativeOperators.map(op => {
        op.processMap(inputFields) match {
          case Some(values) => op.key -> Some(values)
          case None => op.key -> None
        }
      })
    }
      .groupByKey()
      .map { case (dimValues, aggregations) =>
        val aggregatedValues = aggregations.flatMap(aggregationsMap => aggregationsMap)
          .groupBy { case (opKey, opValue) => opKey }
          .map { case (nameOp, valuesOp) =>
            val op = associativeOperatorsMap(nameOp)
            val values = valuesOp.map { case (key, value) => value }

            (nameOp, op.processReduce(values))
          }.toSeq

        (dimValues, (aggregatedValues, UpdatedAggregations))
      }

  //scalastyle:off
  protected def updateAssociativeFunction(values: Seq[(Seq[(String, Option[Any])], Int)],
                                          state: Option[(Map[String, Option[Any]], Int)])
  : Option[(Map[String, Option[Any]], Int)] = {

    val stateWithoutUpdateVar = state match {
      case Some(stateValues) => stateValues._1
      case None => Map()
    }
    val actualState = stateWithoutUpdateVar.toSeq.map { case (key, value) => (key, (Operator.OldValuesKey, value)) }
    val newWithoutUpdateVar = values.map { case (values, isNewValues) => values }
    val newValues = newWithoutUpdateVar.flatten.map { case (key, value) => (key, (Operator.NewValuesKey, value)) }
    val processAssociative = (newValues ++ actualState)
      .groupBy { case (key, value) => key }
      .map { case (opKey, opValues) =>
        associativeOperatorsMap(opKey) match {
          case op: Associative => (opKey, op.associativity(opValues.map { case (nameOp, valuesOp) => valuesOp }))
          case _ => (opKey, None)
        }
      }

    getUpdatedAggregations(processAssociative, values.isEmpty)
  }

  //scalastyle:on

  def noAggregationsState(dimensionsValues: DStream[(DimensionValuesTime, Map[String, JSerializable])])
  : DStream[(DimensionValuesTime, Map[String, Option[Any]])] =
    dimensionsValues.map {
      case (dimensionValueTime, aggregations) => (dimensionValueTime, operators.map(op => op.key -> None).toMap)
    }

  /**
   * Filter dimension values that are been changed in this window
   */

  def filterUpdatedAggregations[T](values: DStream[(DimensionValuesTime, (T, Int))])
  : DStream[(DimensionValuesTime, T)] =
    values.filter { case (dimensions, aggregations) => aggregations._2 == UpdatedAggregations }
      .map { case (dimensions, aggregations) => (dimensions, aggregations._1) }

  /**
   * Return the aggregations with the correct key in case of the actual streaming window have new values for the
   * dimensions values.
   */

  def getUpdatedAggregations[T](aggregations: T, haveNewValues: Boolean): Option[(T, Int)] =
    if (haveNewValues)
      Some(aggregations, NotUpdatedAggregations)
    else Some(aggregations, UpdatedAggregations)
}
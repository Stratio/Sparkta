/**
 * Copyright (C) 2016 Stratio (http://stratio.com)
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

package com.stratio.sparkta.driver.helper

import com.stratio.sparkta.aggregator.{Cube, CubeWriter}
import com.stratio.sparkta.sdk.TypeOp.TypeOp
import com.stratio.sparkta.sdk._
import com.stratio.sparkta.serving.core.helpers.OperationsHelper
import com.stratio.sparkta.serving.core.models._
import org.apache.spark.sql.types._

object SchemaHelper {

  final val Default_Precision = 10
  final val Default_Scale = 0
  final val Nullable = true
  final val NotNullable = false
  final val DefaultTimeStampTypeString = "timestamp"
  final val DefaultTimeStampType = TypeOp.Timestamp

  val mapTypes = Map(
    TypeOp.Long -> LongType,
    TypeOp.Double -> DoubleType,
    TypeOp.BigDecimal -> DecimalType(Default_Precision, Default_Scale),
    TypeOp.Int -> IntegerType,
    TypeOp.Boolean -> BooleanType,
    TypeOp.Date -> DateType,
    TypeOp.DateTime -> TimestampType,
    TypeOp.Timestamp -> TimestampType,
    TypeOp.ArrayDouble -> ArrayType(DoubleType),
    TypeOp.ArrayString -> ArrayType(StringType),
    TypeOp.String -> StringType,
    TypeOp.MapStringLong -> MapType(StringType, LongType)
  )

  def getSchemasFromCubes(cubes: Seq[Cube],
                          cubeModels: Seq[CommonCubeModel],
                          outputModels: Seq[PolicyElementModel]): Seq[TableSchema] = {
    for {
      (cube, cubeModel) <- cubes.zip(cubeModels)
      measuresMerged = (measuresFields(cube.operators) ++ getFixedMeasure(cubeModel.writer)).sortWith(_.name < _.name)
      timeDimension = getExpiringData(cubeModel.checkpointConfig).map(config => config.timeDimension)
      dimensions = filterDimensionsByTime(cube.dimensions.sorted, timeDimension)
      (dimensionsWithId, isAutoCalculatedId) = dimensionFieldsWithId(dimensions, cubeModel.writer)
      dateType = Output.getTimeTypeFromString(cubeModel.writer.fold(DefaultTimeStampTypeString) { options =>
        options.dateType.getOrElse(DefaultTimeStampTypeString)
      })
      structFields = dimensionsWithId ++ timeDimensionFieldType(timeDimension, dateType) ++ measuresMerged
      schema = StructType(structFields)
      outputs = outputsFromOptions(cubeModel, outputModels.map(_.name))
    } yield TableSchema(outputs, cube.name, schema, timeDimension, dateType, isAutoCalculatedId)
  }

  def getExpiringData(checkpointModel: CommonCheckpointModel): Option[ExpiringDataConfig] = {
    val timeName = if (checkpointModel.timeDimension.isEmpty)
      checkpointModel.precision
    else checkpointModel.timeDimension

    timeName.toLowerCase() match {
      case "none" => None
      case _ => Option(ExpiringDataConfig(
        timeName,
        checkpointModel.precision,
        OperationsHelper.parseValueToMilliSeconds(checkpointModel.computeLast.get)))
    }
  }

  // XXX Private methods.

  private def outputsFromOptions(cubeModel: CommonCubeModel, outputsNames: Seq[String]): Seq[String] =
    cubeModel.writer.fold(outputsNames) { writerModel =>
      if (writerModel.outputs.isEmpty) outputsNames else writerModel.outputs
    }

  private def dimensionFieldsWithId(dimensions: Seq[Dimension],
                                    writerModel: Option[WriterModel]): (Seq[StructField], Boolean) = {
    val dimensionFields = dimensionsFields(dimensions)

    writerModel match {
      case Some(writer) => writer.isAutoCalculatedId.fold((dimensionFields, false)) { autoId =>
        if (autoId)
          (Seq(Output.defaultStringField(Output.Id, NotNullable)) ++ dimensionFields.filter(_.name != Output.Id), true)
        else (dimensionFields, false)
      }
      case None => (dimensionFields, false)
    }
  }

  private def filterDimensionsByTime(dimensions: Seq[Dimension], timeDimension: Option[String]): Seq[Dimension] =
    timeDimension match {
      case Some(timeName) => dimensions.filter(dimension => dimension.name != timeName)
      case None => dimensions
    }

  private def rowTypeFromOption(optionType: TypeOp): DataType = mapTypes.getOrElse(optionType, BinaryType)

  private def timeDimensionFieldType(timeDimension: Option[String], dateType: TypeOp.Value): Seq[StructField] = {
    timeDimension match {
      case None => Seq.empty[StructField]
      case Some(timeDimensionName) => Seq(Output.getTimeFieldType(dateType, timeDimensionName, NotNullable))
    }
  }

  private def measuresFields(operators: Seq[Operator]): Seq[StructField] =
    operators.map(operator => StructField(operator.key, rowTypeFromOption(operator.returnType), Nullable))

  private def dimensionsFields(fields: Seq[Dimension]): Seq[StructField] =
    fields.map(field => StructField(field.name, rowTypeFromOption(field.precision.typeOp), NotNullable))

  private def getFixedMeasure(writerModel: Option[WriterModel]): Seq[StructField] =
    writerModel match {
      case Some(writer) => writer.fixedMeasure.fold(Seq.empty[StructField]) { fixedMeasure =>
        fixedMeasure.split(CubeWriter.FixedMeasureSeparator).headOption.fold(Seq.empty[StructField]) { measureName =>
          Seq(Output.defaultStringField(measureName, Nullable))
        }
      }
      case None => Seq.empty[StructField]
    }
}

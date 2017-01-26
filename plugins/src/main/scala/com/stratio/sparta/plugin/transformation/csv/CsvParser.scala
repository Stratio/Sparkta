/*
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.sparta.plugin.transformation.csv

import java.io.{Serializable => JSerializable}

import com.stratio.sparta.sdk.pipeline.transformation.Parser
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import com.stratio.sparta.sdk.properties.models.PropertiesQueriesModel
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType

import scala.util.Try

class CsvParser(order: Integer,
                 inputField: Option[String],
                 outputFields: Seq[String],
                 schema: StructType,
                 properties: Map[String, JSerializable])
  extends Parser(order, inputField, outputFields, schema, properties) {

  val fieldsModel = properties.getPropertiesFields("fields")
  val fieldsSeparator = Try(properties.getString("delimiter")).getOrElse(",")

  //scalastyle:off
  override def parse(row: Row, removeRaw: Boolean): Option[Row] = {
    val inputValue = Option(row.get(inputFieldIndex))
    val newData = {
      inputValue match {
        case Some(value) =>
          val valuesSplitted = {
            value match {
              case valueCast: Array[Byte] => new Predef.String(valueCast)
              case valueCast: String => valueCast
              case _ => value.toString
            }
          }.split(fieldsSeparator)

          if(valuesSplitted.length == fieldsModel.fields.length){
            val valuesParsed = fieldsModel.fields.map(_.name).zip(valuesSplitted).toMap
            outputFields.map { outputField =>
              val outputSchemaValid = outputFieldsSchema.find(field => field.name == outputField)
              outputSchemaValid match {
                case Some(outSchema) =>
                  valuesParsed.get(outSchema.name) match {
                    case Some(valueParsed) =>
                      parseToOutputType(outSchema, valueParsed)
                    case None =>
                      returnNullValue(new IllegalStateException(
                        s"The values parsed not have the schema field: ${outSchema.name}"))
                  }
                case None =>
                  returnNullValue(new IllegalStateException(
                    s"Impossible to parse outputField: $outputField in the schema"))
              }
            }
          } else returnNullValue(new IllegalStateException(s"The values splitted are more or less than properties fields"))
        case None =>
          returnNullValue(new IllegalStateException(s"The input value is null or empty"))
      }
    }
    val prevData = if (removeRaw) row.toSeq.drop(1) else row.toSeq

    Option(Row.fromSeq(prevData ++ newData))
  }

  //scalastyle:on
}

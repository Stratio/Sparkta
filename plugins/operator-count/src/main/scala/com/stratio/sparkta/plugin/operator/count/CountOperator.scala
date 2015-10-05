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

package com.stratio.sparkta.plugin.operator.count

import java.io.{Serializable => JSerializable}
import scala.util.Try

import com.stratio.sparkta.sdk.TypeOp
import com.stratio.sparkta.sdk.TypeOp._
import com.stratio.sparkta.sdk.ValidatingPropertyMap._
import com.stratio.sparkta.sdk._

class CountOperator(name: String, properties: Map[String, JSerializable]) extends Operator(name, properties) {

  val distinctFields = if (properties.contains("distinctFields")) {
    val fields = properties.getString("distinctFields").split(CountOperator.Separator)
    if (fields.isEmpty) None else Some(fields)
  } else None

  override val associative = true

  override val defaultTypeOperation = TypeOp.Long

  override val writeOperation = WriteOp.Inc

  override val castingFilterType = TypeOp.Number

  override def processMap(inputFields: Map[String, JSerializable]): Option[Any] = {
    applyFilters(inputFields).flatMap(filteredFields => distinctFields match {
      case None => CountOperator.SomeOne
      case Some(fields) => Some(fields.map(field => filteredFields.getOrElse(field, CountOperator.NullValue))
        .mkString(CountOperator.Separator).toString)
    })
  }

  override def processReduce(values: Iterable[Option[Any]]): Option[Long] = {
    Try {
      val longList = distinctFields match {
        case None => values.flatten.map(value => value.asInstanceOf[Number].longValue())
        case Some(fields) => values.flatten.toList.distinct.map(value => CountOperator.SomeOne.get)
      }
      Some(longList.sum)
    }.getOrElse(CountOperator.SomeZero)
  }

  override def processAssociative(values: Iterable[Option[Any]]): Option[Long] = {
    Some(transformValueByTypeOp(returnType, values.flatten.map(_.asInstanceOf[Number].longValue()).sum))
  }
}

private object CountOperator {

  val SomeOne = Some(1L)
  val SomeZero = Some(0L)
  val Separator = "_"
  val NullValue = "None"
}

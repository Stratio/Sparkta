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

package com.stratio.sparkta.plugin.operator.variance

import java.io.{Serializable => JSerializable}
import scala.util.Try

import com.stratio.sparkta.sdk.TypeOp._
import com.stratio.sparkta.sdk.{TypeOp, WriteOp, Operator}
import com.stratio.sparkta.sdk.ValidatingPropertyMap._
import breeze.stats._

class VarianceOperator(name: String, properties: Map[String, JSerializable]) extends Operator(name, properties) {

  override val defaultTypeOperation = TypeOp.Double

  private val inputField = if(properties.contains("inputField")) Some(properties.getString("inputField")) else None

  override val writeOperation = WriteOp.Variance

  override val castingFilterType = TypeOp.Number

  override def processMap(inputFields: Map[String, JSerializable]): Option[Number] = {
    if (inputField.isDefined && inputFields.contains(inputField.get))
      applyFilters(inputFields)
        .flatMap(filteredFields => getNumberFromSerializable(filteredFields.get(inputField.get).get))
    else None
  }

  override def processReduce(values: Iterable[Option[Any]]): Option[Double] = {
    val valuesFiltered = getDistinctValues(values.flatten)
    valuesFiltered.size match {
      case (nz) if (nz != 0) =>
        Some(transformValueByTypeOp(returnType, variance(valuesFiltered.map(_.asInstanceOf[Number].doubleValue()))))
      case _ => VarianceOperator.SOME_ZERO
    }
  }
}

private object VarianceOperator {
  val SOME_ZERO = Some(0d)
}


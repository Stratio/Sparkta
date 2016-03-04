/**
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
package com.stratio.sparta.plugin.operator.entityCount

import java.io.{Serializable => JSerializable}

import com.stratio.sparta.sdk.TypeOp._
import com.stratio.sparta.sdk._
import org.apache.spark.sql.types.StructType

import scala.util.Try

class EntityCountOperator(name: String,
                          schema: StructType,
                          properties: Map[String, JSerializable])
  extends OperatorEntityCount(name, schema, properties) with Associative {

  final val Some_Empty = Some(Map("" -> 0L))

  override val defaultTypeOperation = TypeOp.MapStringLong

  override val writeOperation = WriteOp.EntityCount

  override def processReduce(values: Iterable[Option[Any]]): Option[Seq[String]] =
    Try(Option(values.flatten.flatMap(_.asInstanceOf[Seq[String]]).toSeq))
      .getOrElse(None)

  def associativity(values: Iterable[(String, Option[Any])]): Option[Map[String, Long]] = {
    val oldValues = extractValues(values, Option(Operator.OldValuesKey))
      .flatMap(_.asInstanceOf[Map[String, Long]]).toList
    val newValues = applyCount(extractValues(values, Option(Operator.NewValuesKey))
      .flatMap(_.asInstanceOf[Seq[String]]).toList).toList
    val wordCounts = applyCountMerge(oldValues ++ newValues)

    Try(Option(transformValueByTypeOp(returnType, wordCounts)))
      .getOrElse(Option(Map()))
  }

  private def applyCount(values: List[String]): Map[String, Long] =
    values.groupBy((word: String) => word).mapValues(_.length.toLong)

  private def applyCountMerge(values: List[(String, Long)]): Map[String, Long] =
    values.groupBy { case (word, count) => word }.mapValues {
      listValues => listValues.map { case (key, value) => value }.sum
    }
}



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

package com.stratio.sparkta.plugin.field.tag

import java.io.{Serializable => JSerializable}

import akka.event.slf4j.SLF4JLogging

import TagField._
import com.stratio.sparkta.sdk._

case class TagField(props: Map[String, JSerializable]) extends DimensionType with JSerializable with SLF4JLogging {

  def this() {
    this(Map())
  }

  override val defaultTypeOperation = TypeOp.ArrayString

  override val operationProps : Map[String, JSerializable] = props

  override val properties: Map[String, JSerializable] = props

  override val precisions: Map[String, Precision] = Map(
    FirstTagName -> getPrecision(FirstTagName, getTypeOperation(FirstTagName)),
    LastTagName -> getPrecision(LastTagName, getTypeOperation(LastTagName)),
    AllTagsName -> getPrecision(AllTagsName, getTypeOperation(AllTagsName)))

  override def dimensionValues(value: JSerializable): Map[Precision, JSerializable] =
    precisions.map(bt => bt._2 -> TagField.getPrecision(value.asInstanceOf[Iterable[JSerializable]], bt._2))
}

object TagField {

  final val FirstTagName = "fistTag"
  final val LastTagName = "lastTag"
  final val AllTagsName = "allTags"

  def getPrecision(value: Iterable[JSerializable], precision: Precision): JSerializable =
    TypeOp.transformValueByTypeOp(precision.typeOp, precision.id match {
      case name if name == FirstTagName => value.head
      case name if name == LastTagName => value.last
      case name if name == AllTagsName => value.toSeq.asInstanceOf[JSerializable]
    })
}

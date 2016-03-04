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
package com.stratio.sparta.plugin.output.kafka.producer

import java.io.{Serializable => JSerializable}
import java.util.Properties
import com.stratio.sparta.plugin.output.kafka.KafkaOutputFormat
import com.stratio.sparta.sdk.ValidatingPropertyMap._

import kafka.producer.{KeyedMessage, Producer, ProducerConfig}

import scala.collection.mutable
import scala.util.{Success, Failure, Try}

trait KafkaProducer {

  def send(properties: Map[String, JSerializable], topic: String, message: String): Unit = {
    val keyedMessage: KeyedMessage[String, String] = new KeyedMessage[String, String](topic, message)
    KafkaProducer.getProducer(topic, properties).send(keyedMessage)
  }

}

object KafkaProducer {
  private val DefaultHostPort: String = "localhost:9092"
  private val DefaultKafkaSerializer: String = "kafka.serializer.StringEncoder"
  private val DefaultRequiredAcks: String = "0"
  private val DefaultProducerType = "sync"
  private val DefaultBatchNumMessages = "200"

  private val producers: mutable.Map[String, Producer[String, String]] = mutable.Map.empty

  private val getString: ((Map[String, JSerializable], String, String) => String) = (properties, key, default) => {
    properties.get(key) match {
      case Some(v) => v.toString
      case None => throw new IllegalStateException(s"The field $key is mandatory")
    }
  }

  private val getList: ((Map[String, JSerializable], String, String) => String) = (properties, key, default) => {
    Try(properties.getMapFromJsoneyString(key)) match {
      case Success(jsonObject) => {
        val valueAsSeq = jsonObject.map(c =>
          (c.get("host") match {
            case Some(value) => value.toString
            case None => throw new IllegalStateException(s"The field $key is mandatory")
          },
            c.get("port") match {
              case Some(value) => value.toString.toInt
              case None => throw new IllegalStateException(s"The field $key is mandatory")
            }))
        (for (elem <- valueAsSeq) yield s"${elem._1}:${elem._2}").mkString(",")
      }
      case Failure(_) => throw new IllegalStateException(s"The field $key is mandatory")
    }
  }

  private val mandatoryOptions: Map[String, ((Map[String, JSerializable], String, String) => AnyRef, String)] = Map(
    "metadata.broker.list" ->(getList, DefaultHostPort),
    "serializer.class" ->(getString, DefaultKafkaSerializer),
    "request.required.acks" ->(getString, DefaultRequiredAcks),
    "producer.type" ->(getString, DefaultProducerType),
    "batch.num.messages" ->(getString, DefaultBatchNumMessages))


  def extractOptions(properties: Map[String, JSerializable],
                     map: Map[String, ((Map[String, JSerializable], String, String) => AnyRef, String)]): Properties = {
    val props = new Properties()
    map.foreach {
      case (key, (func, default)) =>
        properties.get(key) match {
          case Some(value) => props.put(key, func(properties, key, default))
          case None => props.put(key, default)
        }
    }
    props
  }

  def getProducer(topic: String, properties: Map[String, JSerializable]): Producer[String, String] = {
    getInstance(getProducerKey(topic, properties), properties)
  }

  def getInstance(key: String, properties: Map[String, JSerializable]): Producer[String, String] = {
    producers.getOrElse(key, {
      val producer = createProducer(properties)
      producers.put(key, producer)
      producer
    })
  }

  def createProducer(properties: Map[String, JSerializable]): Producer[String, String] = {
    val props: Properties = extractOptions(properties, mandatoryOptions)
    val producerConfig = new ProducerConfig(props)
    new Producer[String, String](producerConfig)
  }

  def getProducerKey(topic: String, properties: Map[String, JSerializable]): String = {
    s"${getList(properties, "metadata.broker.list", DefaultHostPort)}"
  }
}
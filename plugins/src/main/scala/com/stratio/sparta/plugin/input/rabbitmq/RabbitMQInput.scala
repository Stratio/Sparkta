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
package com.stratio.sparta.plugin.input.rabbitmq

import java.io.{Serializable => JSerializable}

import com.stratio.receiver.RabbitMQUtils
import com.stratio.sparta.sdk.Input
import com.stratio.sparta.sdk.ValidatingPropertyMap._
import org.apache.spark.sql.Row
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

class RabbitMQInput(properties: Map[String, JSerializable]) extends Input(properties) {

  val DefaultRabbitMQPort = "5672"

  val RoutingKeys = getRabbitRoutingKeys("routingKeys")
  val RabbitMQQueueName = properties.getString("queue")
  val RabbitMQHost = properties.getString("host", "localhost")
  val RabbitMQPort = properties.getString("port", DefaultRabbitMQPort).toInt
  val ExchangeName = properties.getString("exchangeName", "")

  def setUp(ssc: StreamingContext, sparkStorageLevel: String): DStream[Row] = {
    RoutingKeys match {
      case Seq() => createStreamFromAQueue(ssc, sparkStorageLevel)
      case _ => createStreamFromRoutingKeys(ssc, sparkStorageLevel)
    }
  }

  private def createStreamFromRoutingKeys(ssc: StreamingContext, sparkStorageLevel: String): DStream[Row] = {
    RabbitMQUtils.createStreamFromRoutingKeys(ssc,
      RabbitMQHost,
      RabbitMQPort,
      ExchangeName,
      RoutingKeys,
      storageLevel(sparkStorageLevel))
      .map(data => Row(data))
  }

  private def createStreamFromAQueue(ssc: StreamingContext, sparkStorageLevel: String): DStream[Row] = {
    RabbitMQUtils.createStreamFromAQueue(ssc,
      RabbitMQHost,
      RabbitMQPort,
      RabbitMQQueueName,
      storageLevel(sparkStorageLevel))
      .map(data => Row(data))
  }

  def getRabbitRoutingKeys(key: String): Seq[String] = {

    if (!properties.hasKey("routingKeys")) {
      Seq()
    } else {
      val conObj = properties.getMapFromJsoneyString(key)
      val routingKeys = conObj.map(routingKeys => routingKeys.get("routingKey").get.toString)
      routingKeys
    }
  }
}

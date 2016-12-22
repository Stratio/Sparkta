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
package com.stratio.sparta.plugin.input.websocket

import java.io.{Serializable => JSerializable}

import com.stratio.sparta.sdk.pipeline.input.Input
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import org.apache.spark.sql.Row
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

class WebSocketInput(properties: Map[String, JSerializable]) extends Input(properties) {

  def setUp(ssc: StreamingContext, sparkStorageLevel: String): DStream[Row] = {
    ssc.receiverStream(new WebSocketReceiver(properties.getString("url"), storageLevel(sparkStorageLevel)))
      .map(data => Row(data))
  }
}

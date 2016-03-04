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
package com.stratio.sparta.plugin.test.input.kafka

import com.stratio.sparta.plugin.input.kafka.KafkaDirectInput
import com.stratio.sparta.sdk.JsoneyString
import org.scalatest.{Matchers, WordSpec}


class DirectTopicPartitionTest extends WordSpec with Matchers {

  "getDirectTopicPartition" should {

    "return a tuples (topic,partition)" in {
      val conn = """[{"topic":"test"}]"""
      val input = new KafkaDirectInput(Map("topics" -> JsoneyString(conn)))
      input.getDirectTopicPartition("topics", "topic") should be("test")
    }

    "return a sequence of tuples (topic,partition)" in {

      val conn =
        """[{"topic":"test"},{"topic":"test2"},{"topic":"test3"}]"""
      val input = new KafkaDirectInput(Map("topics" -> JsoneyString(conn)))
      input.getDirectTopicPartition("topics", "topic") should
        be("test,test2,test3")
    }
  }
}

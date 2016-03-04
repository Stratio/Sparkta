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
package com.stratio.sparta.plugin.input.twitter

import java.io.{Serializable => JSerializable}

import com.google.gson.Gson
import com.stratio.sparta.sdk.Input
import com.stratio.sparta.sdk.ValidatingPropertyMap._
import org.apache.spark.sql.Row
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.twitter.TwitterUtils
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

import scala.util.{Failure, Success, Try}

/**
 * Connects to Twitter's stream and generates stream events.
 */
class TwitterJsonInput(properties: Map[String, JSerializable]) extends Input(properties) {

  System.setProperty("twitter4j.oauth.consumerKey", properties.getString("consumerKey"))
  System.setProperty("twitter4j.oauth.consumerSecret", properties.getString("consumerSecret"))
  System.setProperty("twitter4j.oauth.accessToken", properties.getString("accessToken"))
  System.setProperty("twitter4j.oauth.accessTokenSecret", properties.getString("accessTokenSecret"))

  val cb = new ConfigurationBuilder().setUseSSL(true)
  val tf = new TwitterFactory(cb.build())
  val twitterApi = tf.getInstance()
  val trends = twitterApi.getPlaceTrends(1).getTrends.map(trend => trend.getName)
  val terms: Option[Seq[String]] = Try(properties.getString("termsOfSearch")) match {
    case Success("") => None
    case Success(t: String) => Some(t.split(",").toSeq)
    case Failure(_) => None
  }
  val search = terms.getOrElse(trends.toSeq)

  def setUp(ssc: StreamingContext, sparkStorageLevel: String): DStream[Row] = {
    TwitterUtils.createStream(ssc, None, search, storageLevel(sparkStorageLevel))
      .map(stream => {
        val gson = new Gson()
        Row(gson.toJson(stream))
      }
      )
  }
}

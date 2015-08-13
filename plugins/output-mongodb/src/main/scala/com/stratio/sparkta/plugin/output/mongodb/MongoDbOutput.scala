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

package com.stratio.sparkta.plugin.output.mongodb

import java.io.{Serializable => JSerializable}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SaveMode._

import scala.util.Try

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.conversions.scala._
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.streaming.dstream.DStream
import org.joda.time.DateTime

import com.stratio.sparkta.plugin.output.mongodb.dao.MongoDbDAO
import com.stratio.sparkta.sdk.TypeOp._
import com.stratio.sparkta.sdk.ValidatingPropertyMap._
import com.stratio.sparkta.sdk.WriteOp.WriteOp
import com.stratio.sparkta.sdk._

class MongoDbOutput(keyName: String,
                    properties: Map[String, JSerializable],
                    operationTypes: Option[Map[String, (WriteOp, TypeOp)]],
                    bcSchema: Option[Seq[TableSchema]])
  extends Output(keyName, properties, operationTypes, bcSchema) with MongoDbDAO {

  RegisterJodaTimeConversionHelpers()

  override val isAutoCalculateId = true

  override val mongoClientUri = properties.getString("clientUri", "mongodb://localhost:27017")

  val mongoDbDataFrameConnection = mongoClientUri.replaceAll("mongodb://", "")

  override val dbName = properties.getString("dbName", "sparkta")

  override val connectionsPerHost = Try(properties.getInt("connectionsPerHost")).getOrElse(DefaultConnectionsPerHost)

  override val threadsAllowedB = Try(properties.getInt("threadsAllowedToBlock"))
    .getOrElse(DefaultThreadsAllowedToBlock)

  override val retrySleep = Try(properties.getInt("retrySleep")).getOrElse(DefaultRetrySleep)

  override val idAsField = Try(properties.getString("idAsField").toBoolean).getOrElse(false)

  override val textIndexFields = properties.getString("textIndexFields", None).map(_.split(FieldsSeparator))

  override val language = properties.getString("language", None)

  override def setup: Unit =
    if (bcSchema.isDefined) {
      val schemasFiltered =
        bcSchema.get.filter(schemaFilter => schemaFilter.outputName == keyName).map(getTableSchemaFixedId(_))
      filterSchemaByFixedAndTimeDimensions(schemasFiltered)
        .foreach(tableSchema => createPkTextIndex(tableSchema.tableName, tableSchema.timeDimension))
    }

  override def doPersist(stream: DStream[(DimensionValuesTime, Map[String, Option[Any]])]): Unit = {
    persistDataFrame(stream)
  }

  override def upsert(dataFrame: DataFrame, tableName: String, timeDimension: String): Unit = {
    val options = getDataFrameOptions(tableName, timeDimension)
    dataFrame.write
      .format("com.stratio.provider.mongodb")
      .mode(Append)
      .options(options)
      .save()
  }

  private def getDataFrameOptions(tableName: String, timeDimension: String): Map[String, String] =
    Map(
      "host" -> mongoDbDataFrameConnection,
      "database" -> dbName,
      "collection" -> tableName) ++ getPrimaryKeyOptions(timeDimension) ++ {
      if (language.isDefined) Map("language" -> language.get) else Map()
    }

  private def getPrimaryKeyOptions(timeDimension: String): Map[String, String] =
    if (idAsField) Map("_idField" -> Output.Id)
    else {
      if (!timeDimension.isEmpty) {
        Map("searchFields" -> Seq(Output.Id, timeDimension).mkString(","))
      } else Map()
    }
}


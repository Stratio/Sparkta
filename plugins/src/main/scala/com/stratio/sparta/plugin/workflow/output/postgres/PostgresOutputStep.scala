/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.output.postgres

import java.io.{InputStream, Serializable => JSerializable}

import com.stratio.sparta.plugin.helper.SecurityHelper
import com.stratio.sparta.plugin.helper.SecurityHelper._
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import com.stratio.sparta.sdk.workflow.enumerators.SaveModeEnum
import com.stratio.sparta.sdk.workflow.enumerators.SaveModeEnum.SpartaSaveMode
import com.stratio.sparta.sdk.workflow.step.{ErrorValidations, OutputStep}
import org.apache.spark.sql._
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions
import org.apache.spark.sql.jdbc.SpartaJdbcUtils
import org.apache.spark.sql.jdbc.SpartaJdbcUtils._
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection

import scala.util.{Failure, Success, Try}

class PostgresOutputStep(name: String, xDSession: XDSession, properties: Map[String, JSerializable])
  extends OutputStep(name, xDSession, properties) {

  lazy val url = properties.getString("url", "")
  lazy val delimiter = properties.getString("delimiter", "\t")
  lazy val newLineSubstitution = properties.getString("newLineSubstitution", " ")
  lazy val quotesSubstitution = properties.getString("newQuotesSubstitution", """\b""")
  lazy val encoding = properties.getString("encoding", "UTF8")
  lazy val postgresSaveMode = PostgresSaveMode.withName(properties.getString("postgresSaveMode", "CopyIn").toUpperCase)
  lazy val tlsEnable = Try(properties.getBoolean("tlsEnabled")).getOrElse(false)

  val sparkConf = xDSession.conf.getAll
  val securityUri = getDataStoreUri(sparkConf)
  val urlWithSSL = if (tlsEnable) url + securityUri else url

  override def validate(options: Map[String, String] = Map.empty[String, String]): ErrorValidations = {
    var validation = ErrorValidations(valid = true, messages = Seq.empty)

    if (url.isEmpty)
      validation = ErrorValidations(valid = false, messages = validation.messages :+ s"$name: the url must be provided")
    if (tlsEnable && securityUri.isEmpty)
      validation = ErrorValidations(
        valid = false,
        messages = validation.messages :+ s"$name: when TLS is enable the sparkConf must contain the security options"
      )

    validation
  }

  override def supportedSaveModes: Seq[SpartaSaveMode] =
    Seq(SaveModeEnum.Append, SaveModeEnum.Overwrite, SaveModeEnum.Upsert)

  //scalastyle:off
  override def save(dataFrame: DataFrame, saveMode: SaveModeEnum.Value, options: Map[String, String]): Unit = {
    require(url.nonEmpty, "Postgres url must be provided")
    validateSaveMode(saveMode)
    if(dataFrame.schema.fields.nonEmpty) {
      val tableName = getTableNameFromOptions(options)
      val sparkSaveMode = getSparkSaveMode(saveMode)
      val connectionProperties = new JDBCOptions(urlWithSSL,
        tableName,
        propertiesWithCustom.mapValues(_.toString).filter(_._2.nonEmpty) + ("driver" -> "org.postgresql.Driver")
      )

      Try {
        if (sparkSaveMode == SaveMode.Overwrite)
          SpartaJdbcUtils.dropTable(connectionProperties, name)

        synchronized {
          SpartaJdbcUtils.tableExists(connectionProperties, dataFrame, name)
        }
      } match {
        case Success(tableExists) =>
          try {
            if (tableExists) {
              if (saveMode == SaveModeEnum.Upsert) {
                val updateFields = getPrimaryKeyOptions(options) match {
                  case Some(pk) => pk.trim.split(",").toSeq
                  case None => Seq.empty[String]
                }

                require(updateFields.nonEmpty, "The primary key fields must be provided")
                require(updateFields.forall(dataFrame.schema.fieldNames.contains(_)),
                    "All the primary key fields should be present in the dataFrame schema")
                SpartaJdbcUtils.upsertTable(dataFrame, connectionProperties, updateFields, name)

              } else {
                if (postgresSaveMode == PostgresSaveMode.COPYIN) {
                  dataFrame.foreachPartition { rows =>
                    val conn = getConnection(connectionProperties, name)
                    val cm = new CopyManager(conn.asInstanceOf[BaseConnection])

                    cm.copyIn(
                      s"""COPY $tableName FROM STDIN WITH (NULL 'null', ENCODING '$encoding', FORMAT CSV, DELIMITER E'$delimiter', QUOTE E'$quotesSubstitution')""",
                      rowsToInputStream(rows)
                    )
                  }
                } else {
                  SpartaJdbcUtils.saveTable(dataFrame, connectionProperties, name)
                }
              }
            } else log.warn(s"Table not created in Postgres: $tableName")
          } catch {
            case e: Exception =>
              closeConnection(name)
              log.error(s"Error saving data into Postgres table $tableName with Error: ${e.getLocalizedMessage}")
              throw e
          }
        case Failure(e) =>
          closeConnection(name)
          log.error(s"Error creating/dropping table $tableName with Error: ${e.getLocalizedMessage}")
          throw e
      }
    }
  }

  //scalastyle:on

  def rowsToInputStream(rows: Iterator[Row]): InputStream = {
    val bytes: Iterator[Byte] = rows.flatMap { row =>
      (row.mkString(delimiter).replace("\n", newLineSubstitution) + "\n").getBytes(encoding)
    }

    new InputStream {
      override def read(): Int =
        if (bytes.hasNext) bytes.next & 0xff
        else -1
    }
  }

  override def cleanUp(options: Map[String, String]): Unit = {
    log.info(s"Closing connections in Postgres Output: $name")
    closeConnection(name)
  }
}

object PostgresOutputStep {

  def getSparkSubmitConfiguration(configuration: Map[String, JSerializable]): Seq[(String, String)] = {
    SecurityHelper.dataStoreSecurityConf(configuration)
  }
}
/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.persist

import java.io.{Serializable => JSerializable}

import com.stratio.sparta.plugin.helper.SchemaHelper.parserInputSchema
import com.stratio.sparta.sdk.DistributedMonad
import com.stratio.sparta.sdk.helpers.SdkSchemaHelper
import com.stratio.sparta.sdk.models._
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import com.stratio.sparta.sdk.workflow.step.TransformStep
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext

import scala.util.Try

abstract class PersistTransformStep[Underlying[Row]](
                                                      name: String,
                                                      outputOptions: OutputOptions,
                                                      transformationStepsManagement: TransformationStepManagement,
                                                      ssc: Option[StreamingContext],
                                                      xDSession: XDSession,
                                                      properties: Map[String, JSerializable])
                                                    (implicit dsMonadEvidence: Underlying[Row] => DistributedMonad[Underlying])
  extends TransformStep[Underlying](name, outputOptions, transformationStepsManagement, ssc, xDSession, properties) {

  lazy val storageLevel: Option[StorageLevel] = properties.getString("storageLevel", None).notBlank
    .flatMap(level => Try(StorageLevel.fromString(level)).toOption)

  override def validate(options: Map[String, String] = Map.empty[String, String]): ErrorValidations = {
    var validation = ErrorValidations(valid = true, messages = Seq.empty)

    if (!SdkSchemaHelper.isCorrectTableName(name))
      validation = ErrorValidations(
        valid = false,
        messages = validation.messages :+ WorkflowValidationMessage(s"The step name $name is not valid.", name))

    //If contains schemas, validate if it can be parsed
    if (inputsModel.inputSchemas.nonEmpty) {
      inputsModel.inputSchemas.foreach { input =>
        if (parserInputSchema(input.schema).isFailure)
          validation = ErrorValidations(
            valid = false,
            messages = validation.messages :+ WorkflowValidationMessage(s"The input schema from step ${input.stepName} is not valid.", name))
      }

      inputsModel.inputSchemas.filterNot(is => SdkSchemaHelper.isCorrectTableName(is.stepName)).foreach { is =>
        validation = ErrorValidations(
          valid = false,
          messages = validation.messages :+ WorkflowValidationMessage(s"The input table name ${is.stepName} is not valid.", name))
      }
    }

    validation
  }

  def transformFunc: RDD[Row] => RDD[Row] = {
    case rdd1 =>
      rdd1.persist(storageLevel.getOrElse(StorageLevel.MEMORY_ONLY_SER))
  }
}

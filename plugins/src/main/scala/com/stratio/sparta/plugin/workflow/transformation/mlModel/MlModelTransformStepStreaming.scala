/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.mlModel

import java.io.{Serializable => JSerializable}

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.core.DistributedMonad
import com.stratio.sparta.core.DistributedMonad.Implicits._
import com.stratio.sparta.core.models.{OutputOptions, TransformationStepManagement}
import com.stratio.sparta.plugin.helper.SchemaHelper._
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

class MlModelTransformStepStreaming(
                                         name: String,
                                         outputOptions: OutputOptions,
                                         transformationStepsManagement: TransformationStepManagement,
                                         ssc: Option[StreamingContext],
                                         xDSession: XDSession,
                                         properties: Map[String, JSerializable]
                                       )
  extends MlModelTransformStep[DStream](
    name, outputOptions, transformationStepsManagement, ssc, xDSession, properties) with SLF4JLogging {

  override def transform(inputData: Map[String, DistributedMonad[DStream]]): DistributedMonad[DStream] =
    applyHeadTransform(inputData) { (inputSchema, inputStream) =>
      inputStream.ds.transform { rdd =>
        executeMlModel(inputSchema, rdd)._1
      }
    }
}
/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

package com.stratio.sparta.serving.core.dao

import com.stratio.sparta.core.properties.JsoneyString
import com.stratio.sparta.serving.core.models.SpartaSerializer
import com.stratio.sparta.serving.core.models.enumerators.WorkflowExecutionEngine.ExecutionEngine
import com.stratio.sparta.serving.core.models.parameters.ParameterVariable
import com.stratio.sparta.serving.core.models.workflow._
import org.joda.time.DateTime
import org.json4s.jackson.Serialization._
import com.stratio.sparta.core.models.DebugResults

import scala.util.Try

object CustomColumnTypes extends SpartaSerializer {

  import slick.jdbc.PostgresProfile.api._

  implicit val jsoneyType = MappedColumnType.base[Map[String, JsoneyString], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[Map[String, JsoneyString]](objToDeSerialize)
  )

  implicit val mapType = MappedColumnType.base[Map[String, String], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[Map[String, String]](objToDeSerialize)
  )

  implicit val jodaDateTimeType = MappedColumnType.base[DateTime, java.sql.Timestamp](
    objToSerialize => new java.sql.Timestamp(objToSerialize.getMillis),
    objToDeSerialize => new DateTime(objToDeSerialize.getTime)
  )

  implicit val executionEngineType = MappedColumnType.base[ExecutionEngine, String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[ExecutionEngine](objToDeSerialize)
  )

  implicit val executionPipelineGraph = MappedColumnType.base[PipelineGraph, String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[PipelineGraph](objToDeSerialize)
  )

  implicit val executionUiSettings = MappedColumnType.base[UiSettings, String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[UiSettings](objToDeSerialize)
  )

  implicit val executionSettings = MappedColumnType.base[Settings, String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[Settings](objToDeSerialize)
  )

  implicit val groupType = MappedColumnType.base[Group, String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[Group](objToDeSerialize)
  )

  implicit val executionEngineSeqType = MappedColumnType.base[Seq[ExecutionEngine], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[Seq[ExecutionEngine]](objToDeSerialize)
  )

  implicit val stringListMapper = MappedColumnType.base[Seq[String], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[Seq[String]](objToDeSerialize)
  )

  implicit val parameterVariableSeqType = MappedColumnType.base[Seq[ParameterVariable], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[Seq[ParameterVariable]](objToDeSerialize)
  )

  implicit val executionStatusesType = MappedColumnType.base[Seq[ExecutionStatus], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[Seq[ExecutionStatus]](objToDeSerialize)
  )

  implicit val genericDataExecutionType =  MappedColumnType.base[GenericDataExecution, String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[GenericDataExecution](objToDeSerialize)
  )

  implicit val sparkSubmitExecutionType =  MappedColumnType.base[Option[SparkSubmitExecution], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => Try(read[Option[SparkSubmitExecution]](objToDeSerialize)).getOrElse(None)
  )

  implicit val sparkExecutionType =  MappedColumnType.base[Option[SparkExecution], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => Try(read[Option[SparkExecution]](objToDeSerialize)).getOrElse(None)
  )

  implicit val sparkDispatcherExecutionType =  MappedColumnType.base[Option[SparkDispatcherExecution], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => Try(read[Option[SparkDispatcherExecution]](objToDeSerialize)).getOrElse(None)
  )

  implicit val marathonExecutionType =  MappedColumnType.base[Option[MarathonExecution], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => Try(read[Option[MarathonExecution]](objToDeSerialize)).getOrElse(None)
  )

  implicit val localExecutionType =  MappedColumnType.base[Option[LocalExecution], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => Try(read[Option[LocalExecution]](objToDeSerialize)).getOrElse(None)
  )

  implicit val workflowDebugType = MappedColumnType.base[Workflow, String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => read[Workflow](objToDeSerialize)
  )

  implicit val workflowDebugResultsType = MappedColumnType.base[Option[DebugResults], String](
    objToSerialize => write(objToSerialize),
    objToDeSerialize => Try(read[Option[DebugResults]](objToDeSerialize)).getOrElse(None)
  )

}
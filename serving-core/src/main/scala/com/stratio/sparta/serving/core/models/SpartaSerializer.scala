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
package com.stratio.sparta.serving.core.models


import akka.actor.{ActorRef, ActorSystem}
import akka.event.slf4j.SLF4JLogging
import akka.pattern.ask
import akka.util.Timeout
import com.stratio.sparta.sdk.properties.{EnvironmentContext, JsoneyStringSerializer}
import com.stratio.sparta.sdk.workflow.enumerators._
import com.stratio.sparta.serving.core.actor.EnvironmentListenerActor.GetEnvironment
import com.stratio.sparta.serving.core.constants.{AkkaConstant, AppConstant}
import com.stratio.sparta.serving.core.models.enumerators.{ArityValueEnum, NodeArityEnum, WorkflowExecutionEngine, WorkflowStatusEnum}
import com.stratio.sparta.serving.core.models.workflow.PhaseEnum
import org.json4s.ext.{DateTimeSerializer, EnumNameSerializer}
import org.json4s.{DefaultFormats, Formats}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * Extends this interface if you need serialize / unserialize Sparta's enums in any class / object.
  */
trait SpartaSerializer {

  val serializerSystem: Option[ActorSystem] = None
  val environmentStateActor: Option[ActorRef] = None

  implicit def json4sJacksonFormats: Formats = {
    val environmentContext = (serializerSystem, environmentStateActor) match {
      case (Some(system), Some(envStateActor)) =>
        SpartaSerializer.getEnvironmentContext(system, envStateActor)
      case _ => None
    }

    DefaultFormats + DateTimeSerializer +
      new JsoneyStringSerializer(environmentContext) +
      new EnumNameSerializer(WorkflowStatusEnum) +
      new EnumNameSerializer(NodeArityEnum) +
      new EnumNameSerializer(ArityValueEnum) +
      new EnumNameSerializer(SaveModeEnum) +
      new EnumNameSerializer(InputFormatEnum) +
      new EnumNameSerializer(OutputFormatEnum) +
      new EnumNameSerializer(WhenError) +
      new EnumNameSerializer(WhenRowError) +
      new EnumNameSerializer(WhenFieldError) +
      new EnumNameSerializer(WorkflowExecutionEngine) +
      new EnumNameSerializer(PhaseEnum)
  }

}

object SpartaSerializer extends SLF4JLogging {

  private var environmentContext: Option[EnvironmentContext] = None

  def getEnvironmentContext(actorSystem: ActorSystem, envStateActor: ActorRef): Option[EnvironmentContext] = {
    implicit val system: ActorSystem = actorSystem
    implicit val timeout: Timeout = Timeout(AppConstant.DefaultSerializationTimeout.seconds)

    Try {
      val future = envStateActor ? GetEnvironment
      Await.result(future, timeout.duration).asInstanceOf[Map[String, String]]
    } match {
      case Success(newEnvironment) =>
        environmentContext = Option(EnvironmentContext(newEnvironment))
        environmentContext
      case Failure(e) =>
        log.warn("No environment result, returning the last value", e)
        environmentContext
    }
  }

}

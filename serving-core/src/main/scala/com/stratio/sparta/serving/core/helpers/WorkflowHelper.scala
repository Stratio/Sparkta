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

package com.stratio.sparta.serving.core.helpers

import java.io.Serializable

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.sdk.utils.ClasspathUtils
import com.stratio.sparta.serving.core.constants.AppConstant
import com.stratio.sparta.serving.core.constants.MarathonConstant.DcosServiceName
import com.stratio.sparta.serving.core.models.workflow.{NodeGraph, Workflow}

import scala.util.{Failure, Properties, Success, Try}

object WorkflowHelper extends SLF4JLogging {

  lazy val classpathUtils = new ClasspathUtils

  val OutputStepErrorProperty = "errorSink"

  def getConfigurationsFromObjects(elements: Seq[NodeGraph], methodName: String): Map[String, String] = {
    log.debug("Initializing reflection ...")
    elements.flatMap { o =>
      Try {
        val classType = o.configuration.getOrElse(AppConstant.CustomTypeKey, o.className).toString
        val clazzToInstance = classpathUtils.defaultStepsInClasspath.getOrElse(classType, o.className)
        val clazz = Class.forName(clazzToInstance)
        clazz.getMethods.find(p => p.getName == methodName) match {
          case Some(method) =>
            method.setAccessible(true)
            method.invoke(clazz, o.configuration.asInstanceOf[Map[String, Serializable]])
              .asInstanceOf[Seq[(String, String)]]
          case None =>
            Seq.empty[(String, String)]
        }
      } match {
        case Success(configurations) =>
          configurations
        case Failure(e) =>
          log.warn(s"Error obtaining configurations from singleton objects. ${e.getLocalizedMessage}")
          Seq.empty[(String, String)]
      }
    }.toMap
  }

  private[serving] def retrieveGroup(group: String): String = {
    val reg = "(?!^/)(.*)(?<!/$)".r
    Try(reg.findAllMatchIn(group).next.matched).getOrElse(group)
  }

  def getMarathonId(wfModel: Workflow): String = {
    val inputServiceName = Properties.envOrElse(DcosServiceName, "undefined")
    s"sparta/$inputServiceName/workflows/${retrieveGroup(wfModel.group.name)}" +
      s"/${wfModel.name}/${wfModel.name}-v${wfModel.version}"
  }
}

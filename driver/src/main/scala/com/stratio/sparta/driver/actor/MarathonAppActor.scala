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

package com.stratio.sparta.driver.actor

import akka.actor.{Actor, Props}
import com.stratio.sparta.driver.actor.MarathonAppActor.{StartApp, StopApp}
import com.stratio.sparta.serving.core.actor.ClusterLauncherActor
import com.stratio.sparta.serving.core.actor.LauncherActor.StartWithRequest
import com.stratio.sparta.serving.core.constants.AkkaConstant._
import com.stratio.sparta.serving.core.models.enumerators.PolicyStatusEnum._
import com.stratio.sparta.serving.core.models.workflow.{PhaseEnum, WorkflowErrorModel, WorkflowStatusModel}
import com.stratio.sparta.serving.core.utils.{FragmentUtils, PolicyStatusUtils, PolicyUtils, RequestUtils}
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.NodeCache

import scala.util.{Failure, Success, Try}

class MarathonAppActor(val curatorFramework: CuratorFramework) extends Actor
  with PolicyStatusUtils with FragmentUtils with RequestUtils with PolicyUtils {

  def receive: PartialFunction[Any, Unit] = {
    case StartApp(workflowId) => doStartApp(workflowId)
    case StopApp => preStopActions()
    case _ => log.info("Unrecognized message in Workflow App Actor")
  }

  def preStopActions(): Unit = {
    log.info("Shutting down Sparta Marathon Actor system")
    //Await.ready(context.system.terminate(), 1 minute)
    context.system.shutdown()
  }

  //scalastyle:off
  def doStartApp(workflowId: String): Unit = {
    Try {
      log.debug(s"Obtaining status with workflow id: $workflowId")
      findStatusById(workflowId) match {
        case Success(status) =>
          log.debug(s"Obtained status: ${status.status}")
          if (status.status != Stopped && status.status != Stopping && status.status != Failed &&
            status.status != Finished) {
            log.debug(s"Obtaining workflow and related fragments with id: $workflowId")
            val workflow = getPolicyWithFragments(getPolicyById(workflowId))
            log.debug(s"Obtained workflow: ${workflow.toString}")
            log.debug(s"Closing checker with id: $workflowId and name: ${workflow.name}")
            closeChecker(workflow.id.get, workflow.name)
            log.debug(s"Obtaining request with workflow id: $workflowId")
            findRequestById(workflowId) match {
              case Success(submitRequest) =>
                log.debug(s"Starting request: ${submitRequest.toString}")
                val clusterLauncherActor =
                  context.actorOf(Props(new ClusterLauncherActor(curatorFramework)), ClusterLauncherActorName)
                clusterLauncherActor ! StartWithRequest(workflow, submitRequest)
              case Failure(exception) => throw exception
            }
          } else {
            val information = s"Workflow App launched by Marathon with incorrect state, the job was not executed"
            log.warn(information)
            preStopActions()
            updateStatus(WorkflowStatusModel(id = workflowId, status = Stopped, statusInfo = Option(information),
              lastError = Option(WorkflowErrorModel(information, PhaseEnum.Execution, ""))))
          }
        case Failure(e) => throw e
      }
    } match {
      case Success(_) =>
        log.info(s"StartApp in Workflow App executed without errors")
      case Failure(exception) =>
        val information = s"Error executing Spark Submit in Workflow App"
        log.error(information, exception)
        preStopActions()
        updateStatus(WorkflowStatusModel(id = workflowId, status = Failed, statusInfo = Option(information),
          lastError = Option(WorkflowErrorModel(information, PhaseEnum.Execution, exception.toString))))
    }
  }

  //scalastyle:on

  def closeChecker(workflowId: String, workflowName: String): Unit = {
    log.info(s"Listener added to $workflowName with id: $workflowId")
    addListener(workflowId, (workflowStatus: WorkflowStatusModel, nodeCache: NodeCache) => {
      synchronized {
        if (workflowStatus.status == Stopped || workflowStatus.status == Failed) {
          try {
            val information = s"Executing pre-close actions in Workflow App ..."
            log.info(information)
            preStopActions()
          } finally {
            Try(nodeCache.close()) match {
              case Success(_) =>
                log.info("Workflow App Listener node cache closed correctly")
              case Failure(e) =>
                log.error(s"Workflow App node cache not properly closed", e)
            }
          }
        }
      }
    })
  }
}

object MarathonAppActor {

  case class StartApp(workflowId: String)

  case object StopApp

}
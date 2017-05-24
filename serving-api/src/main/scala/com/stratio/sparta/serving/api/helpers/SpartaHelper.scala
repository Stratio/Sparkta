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

package com.stratio.sparta.serving.api.helpers

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.SLF4JLogging
import akka.io.IO
import com.stratio.sparkta.serving.api.ssl.SSLSupport
import com.stratio.sparta.driver.service.StreamingContextService
import com.stratio.sparta.serving.api.actor._
import com.stratio.sparta.serving.core.actor.StatusActor.AddClusterListeners
import com.stratio.sparta.serving.core.actor.{FragmentActor, RequestActor, StatusActor}
import com.stratio.sparta.serving.core.config.SpartaConfig
import com.stratio.sparta.serving.core.constants.AkkaConstant._
import com.stratio.sparta.serving.core.curator.CuratorFactoryHolder
import com.stratio.sparta.serving.core.helpers.SecurityManagerHelper
import spray.can.Http

/**
 * Helper with common operations used to create a Sparta context used to run the application.
 */
object SpartaHelper extends SLF4JLogging with SSLSupport {

  /**
   * Initializes Sparta's akka system running an embedded http server with the REST API.
   *
   * @param appName with the name of the application.
   */
  def initSpartaAPI(appName: String): Unit = {
    if (SpartaConfig.mainConfig.isDefined && SpartaConfig.apiConfig.isDefined) {
      val curatorFramework = CuratorFactoryHolder.getInstance()

      log.info("Initializing Sparta Actors System ...")
      implicit val system = ActorSystem(appName, SpartaConfig.mainConfig)

      val secManager = SecurityManagerHelper.securityManager
      val statusActor = system.actorOf(Props(new StatusActor(curatorFramework, secManager)), StatusActorName)
      val fragmentActor = system.actorOf(Props(new FragmentActor(curatorFramework, secManager)), FragmentActorName)
      val policyActor = system.actorOf(Props(new PolicyActor(curatorFramework, statusActor, secManager)),
        PolicyActorName)
      val executionActor = system.actorOf(Props(new RequestActor(curatorFramework, secManager)), ExecutionActorName)
      val scService = StreamingContextService(curatorFramework, SpartaConfig.mainConfig)
      val launcherActor = system.actorOf(Props(new LauncherActor(scService, curatorFramework, secManager)),
        LauncherActorName)
      val pluginActor = system.actorOf(Props(new PluginActor(secManager)), PluginActorName)
      val driverActor = system.actorOf(Props(new DriverActor(secManager)), DriverActorName)
      val actors = Map(
        StatusActorName -> statusActor,
        FragmentActorName -> fragmentActor,
        PolicyActorName -> policyActor,
        LauncherActorName -> launcherActor,
        PluginActorName -> pluginActor,
        DriverActorName -> driverActor,
        ExecutionActorName -> executionActor
      )
      val controllerActor = system.actorOf(Props(new ControllerActor(actors, curatorFramework)), ControllerActorName)

      IO(Http) ! Http.Bind(controllerActor,
        interface = SpartaConfig.apiConfig.get.getString("host"),
        port = SpartaConfig.apiConfig.get.getInt("port")
      )
      log.info("Sparta Actors System initiated correctly")

      statusActor ! AddClusterListeners
    } else log.info("Sparta Configuration is not defined")
  }

}

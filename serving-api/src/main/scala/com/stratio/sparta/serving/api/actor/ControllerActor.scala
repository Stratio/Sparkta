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
package com.stratio.sparta.serving.api.actor

import akka.actor.{ActorContext, _}
import akka.event.slf4j.SLF4JLogging
import com.stratio.sparkta.serving.api.service.http.{PolicyContextHttpService, TemplateHttpService}
import com.stratio.sparta.serving.api.headers.{CacheSupport, CorsSupport}
import com.stratio.sparta.serving.api.service.handler.CustomExceptionHandler._
import com.stratio.sparta.serving.api.service.http._
import com.stratio.sparta.serving.core.config.SpartaConfig
import com.stratio.sparta.serving.core.constants.{AkkaConstant, AppConstant}
import com.stratio.sparta.serving.core.models.SpartaSerializer
import com.stratio.spray.oauth2.client.OauthClient
import org.apache.curator.framework.CuratorFramework
import spray.routing._

import scala.util.Try

class ControllerActor(actorsMap: Map[String, ActorRef], curatorFramework: CuratorFramework) extends HttpServiceActor
  with SLF4JLogging
  with SpartaSerializer
  with CorsSupport
  with CacheSupport
  with OauthClient {

  override implicit def actorRefFactory: ActorContext = context

  val serviceRoutes: ServiceRoutes = new ServiceRoutes(actorsMap, context, curatorFramework)

  def receive: Receive = runRoute(handleExceptions(exceptionHandler)(getRoutes))

  def getRoutes: Route = cors {
    secRoute ~ webRoutes ~
      authorized { user =>
        serviceRoutes.fragmentRoute ~
          serviceRoutes.policyContextRoute ~ serviceRoutes.policyRoute ~
          serviceRoutes.templateRoute ~ serviceRoutes.AppStatusRoute ~
          serviceRoutes.pluginsRoute
      }
  }

  def webRoutes: Route =
    get {
      pathPrefix(AppConstant.PluginsURLLocation) {
        getFromDirectory(
          Try(SpartaConfig.getDetailConfig.get.getString(AppConstant.PluginsPackageLocation))
            .getOrElse(AppConstant.DefaultPluginsPackageLocation))
      } ~
        pathPrefix("driverJar") {
          getFromDirectory(
            Try(SpartaConfig.getDetailConfig.get.getString(AppConstant.DriverPackageLocation))
              .getOrElse(AppConstant.DefaultDriverPackageLocation))
        } ~ pathPrefix("") {
        pathEndOrSingleSlash {
          noCache {
            secured { user =>
              getFromResource("classes/web/index.html")
            }
          }
        }
      } ~ getFromResourceDirectory("classes/web") ~
        pathPrefix("") {
          pathEndOrSingleSlash {
            noCache {
              secured { user =>
                getFromResource("web/index.html")
              }
            }
          }
        } ~ getFromResourceDirectory("web")
    }
}

class ServiceRoutes(actorsMap: Map[String, ActorRef], context: ActorContext, curatorFramework: CuratorFramework) {

  val fragmentRoute: Route = new FragmentHttpService {
    implicit val actors = actorsMap
    override val supervisor = actorsMap(AkkaConstant.FragmentActor)
    override val actorRefFactory: ActorRefFactory = context
  }.routes

  val templateRoute: Route = new TemplateHttpService {
    implicit val actors = actorsMap
    override val supervisor = actorsMap(AkkaConstant.TemplateActor)
    override val actorRefFactory: ActorRefFactory = context
  }.routes

  val policyRoute: Route = new PolicyHttpService {
    implicit val actors = actorsMap
    override val supervisor = actorsMap(AkkaConstant.PolicyActor)
    override val actorRefFactory: ActorRefFactory = context
  }.routes

  val policyContextRoute: Route = new PolicyContextHttpService {
    implicit val actors = actorsMap
    override val supervisor = actorsMap(AkkaConstant.SparkStreamingContextActor)
    override val actorRefFactory: ActorRefFactory = context
  }.routes

  val AppStatusRoute: Route = new AppStatusHttpService {
    override implicit val actors: Map[String, ActorRef] = actorsMap
    override val supervisor: ActorRef = context.self
    override val actorRefFactory: ActorRefFactory = context
    override val curatorInstance = curatorFramework
  }.routes

  val pluginsRoute: Route = new PluginsHttpService {
    override implicit val actors: Map[String, ActorRef] = actorsMap
    override val supervisor: ActorRef = actorsMap(AkkaConstant.PluginActor)
    override val actorRefFactory: ActorRefFactory = context
  }.routes
}

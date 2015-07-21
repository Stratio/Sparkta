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

package com.stratio.sparkta.serving.api.actor

import akka.actor._
import akka.event.slf4j.SLF4JLogging
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.stratio.sparkta.driver.models.{ErrorModel, StreamingContextStatusEnum}
import com.stratio.sparkta.driver.service.StreamingContextService
import com.stratio.sparkta.sdk.JsoneyStringSerializer
import com.stratio.sparkta.serving.api.service.http.{FragmentHttpService, JobServerHttpService, PolicyHttpService, TemplateHttpService}
import com.typesafe.config.Config
import com.wordnik.swagger.model.ApiInfo
import org.apache.curator.framework.CuratorFramework
import org.json4s.DefaultFormats
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import spray.http.StatusCodes
import spray.routing._
import spray.util.LoggingContext

import scala.reflect.runtime.universe._

class ControllerActor(streamingContextService: StreamingContextService,
                      curatorFramework: CuratorFramework,
                      configJobServer: Config) extends HttpServiceActor with SLF4JLogging {

  override implicit def actorRefFactory: ActorContext = context

  implicit val json4sJacksonFormats = DefaultFormats +
    new EnumNameSerializer(StreamingContextStatusEnum) +
    new JsoneyStringSerializer()

  implicit def exceptionHandler(implicit logg: LoggingContext): ExceptionHandler =
    ExceptionHandler {
      case exception: Exception =>
        requestUri { uri =>
          logg.error(exception.getLocalizedMessage, exception)
          complete(StatusCodes.NotFound, write(ErrorModel("Error", exception.getLocalizedMessage)))
        }
    }

  def receive: Receive = runRoute(handleExceptions(exceptionHandler)(
    serviceRoutes ~
      swaggerService ~
      swaggerUIroutes
  ))

  val jobServerActor =
    if(!configJobServer.isEmpty && !configJobServer.getString("host").isEmpty && configJobServer.getInt("port") > 0) {
    Some(context.actorOf(Props(new JobServerActor(configJobServer.getString("host"),
      configJobServer.getInt("port"))), "jobServerActor"))
    } else None

  val jobServerService = jobServerActor match {
    case Some(actorRef) => new JobServerHttpService {
      override val supervisor = actorRef
      override implicit def actorRefFactory: ActorRefFactory = context
    }.routes
    case None => new StandardRoute {
      override def apply(v1: RequestContext): Unit = ???
    }
  }

  val serviceRoutes: Route =
    jobServerService ~
      new PolicyHttpService {
        val streamingActor = context.actorOf(Props(new StreamingActor(streamingContextService, jobServerActor)),
          "streamingActor")
        override val supervisor = streamingActor

        override implicit def actorRefFactory: ActorRefFactory = context
      }.routes ~
      new FragmentHttpService {
        val fragmentActor = context.actorOf(Props(new FragmentActor(curatorFramework)), "fragmentActor")
        override val supervisor = fragmentActor

        override implicit def actorRefFactory: ActorRefFactory = context
      }.routes ~
      new TemplateHttpService {
        val templateActor = context.actorOf(Props(new TemplateActor()), "templateActor")
        override val supervisor = templateActor

        override implicit def actorRefFactory: ActorRefFactory = context
      }.routes

  def swaggerUIroutes: Route =
    get {
      pathPrefix("swagger") {
        pathEndOrSingleSlash {
          getFromResource("swagger-ui/index.html")
        }
      } ~ getFromResourceDirectory("swagger-ui")
    }

  val swaggerService = new SwaggerHttpService {
    override def apiTypes: Seq[Type] = Seq(
      typeOf[JobServerHttpService],
      typeOf[PolicyHttpService],
      typeOf[FragmentHttpService],
      typeOf[TemplateHttpService])

    override def apiVersion: String = "1.0"

    override def baseUrl: String = "/"

    // let swagger-ui determine the host and port
    override def docsPath: String = "api-docs"

    override def actorRefFactory: ActorContext = context

    override def apiInfo: Option[ApiInfo] = Some(new ApiInfo(
      "SpaRkTA",
      "A real time aggregation engine full spark based.",
      "TOC Url",
      "Sparkta@stratio.com",
      "Apache V2",
      "http://www.apache.org/licenses/LICENSE-2.0"
    ))
  }.routes
}

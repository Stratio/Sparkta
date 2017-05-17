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

package com.stratio.sparta.serving.api.service.http

import com.github.swagger.spray.{SwaggerHttpService, model}
import com.github.swagger.spray.model._

import scala.reflect.runtime.universe._

trait SwaggerService extends SwaggerHttpService {

  override val apiTypes: Seq[Type] = Seq(
    typeOf[FragmentHttpService],
    typeOf[PolicyHttpService],
    typeOf[PolicyContextHttpService],
    typeOf[PluginsHttpService],
    typeOf[DriverHttpService],
    typeOf[AppStatusHttpService],
    typeOf[ExecutionHttpService]
  )

  // let swagger-ui determine the host and port
  override val apiDocsPath: String = "api-docs"

  override val info= Info(
    description = "Sparta",
    version = "1.0",
    title = "A real time spark-based analytics engine spark based",
    contact = Option(Contact("Sparta","http://www.stratio.com/","sparta@stratio.com")),
    license = Option(License("Apache V2","http://www.apache.org/licenses/LICENSE-2.0"))
  )
  /*
  override def apiInfo: Option[ApiInfo] = Some(ApiInfo(
    "SpaRTA",
    "A real time aggregation engine full spark based.",
    "",
    "Sparta@stratio.com",
    "Apache V2",
    "http://www.apache.org/licenses/LICENSE-2.0"
  ))*/

}

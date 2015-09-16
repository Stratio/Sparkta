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

package com.stratio.sparkta.serving.api.service.http

import akka.pattern.ask
import com.stratio.sparkta.serving.api.constants.HttpConstant
import com.stratio.sparkta.serving.core.models.TemplateModel
import com.wordnik.swagger.annotations._
import spray.routing._

import com.stratio.sparkta.serving.api.actor.TemplateActor._

@Api(value = HttpConstant.TemplatePath,
  description = "Operations about templates. One template will have an abstract" +
    " element that represents a validation, a tip, an icon over it.")
trait TemplateHttpService extends BaseHttpService {

  override def routes: Route = findByType ~ findByTypeAndName

  @ApiOperation(value = "Find all templates depending ot its type. (input|output)",
    notes             = "Find all templates depending ot its type. (input|output)",
    httpMethod        = "GET",
    response          = classOf[TemplateModel],
    responseContainer = "List")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name      = "templateType",
      value     = "type of the template.",
      dataType  = "string",
      paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code    = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)
  ))
  def findByType: Route = {
    path(HttpConstant.TemplatePath / Segment) { (templateType) =>
      get {
        complete {
          for {
            response <- (supervisor ? new FindByType(templateType)).mapTo[ResponseTemplates]
          } yield response.templates.get
        }
      }
    }
  }

  @ApiOperation(value = "Find a template depending ot its type and name. (input|output)",
    notes = "Returns a template.",
    httpMethod = "GET",
    response = classOf[TemplateModel])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name      = "templateType",
      value     = "type of the template.",
      dataType  = "string",
      paramType = "path"),
    new ApiImplicitParam(name      = "name",
      value     = "name of the template",
      dataType  = "string",
      paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)
  ))
  def findByTypeAndName: Route = {
    path(HttpConstant.TemplatePath / Segment / Segment ) { (templateType, name) =>
      get {
        complete {
          for {
            response <- (supervisor ? new FindByTypeAndName(templateType, name)).mapTo[ResponseTemplate]
          } yield response.template.get
        }
      }
    }
  }
}
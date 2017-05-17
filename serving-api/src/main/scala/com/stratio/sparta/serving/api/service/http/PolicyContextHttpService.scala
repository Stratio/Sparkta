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

import akka.actor.ActorRef
import akka.pattern.ask
import com.stratio.sparta.serving.api.constants.HttpConstant
import com.stratio.sparta.serving.core.actor.FragmentActor
import com.stratio.sparta.serving.core.actor.LauncherActor.Launch
import com.stratio.sparta.serving.core.actor.StatusActor.{Delete, FindAll, _}
import com.stratio.sparta.serving.core.constants.AkkaConstant
import com.stratio.sparta.serving.core.exception.ServingCoreException
import com.stratio.sparta.serving.core.helpers.FragmentsHelper
import com.stratio.sparta.serving.core.models._
import com.stratio.sparta.serving.core.models.dto.LoggedUser
import com.stratio.sparta.serving.core.models.policy._
import com.stratio.sparta.serving.core.models.policy.fragment.{FragmentElementModel, FragmentType}
import io.swagger.annotations._
import spray.http.{HttpResponse, StatusCodes}
import spray.routing._

import scala.util.{Failure, Success, Try}

@Api(value = HttpConstant.PolicyContextPath, description = "Operations about policy contexts.", position = 0)
trait PolicyContextHttpService extends BaseHttpService {

  override def routes(user: Option[LoggedUser] = None): Route = findAll(user) ~
    update(user) ~ create(user) ~ deleteAll(user) ~ deleteById(user) ~ find(user)

  @ApiOperation(value = "Finds all policy contexts",
    notes = "Returns a policies list",
    httpMethod = "GET",
    response = classOf[Try[Seq[PolicyStatusModel]]],
    responseContainer = "List")
  @ApiResponses(
    Array(new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)))
  def findAll(user: Option[LoggedUser]): Route = {
    path(HttpConstant.PolicyContextPath) {
      get {
        complete {
          val statusActor = actors(AkkaConstant.StatusActorName)
          for {
            policiesStatuses <- (statusActor ? FindAll).mapTo[Try[Seq[PolicyStatusModel]]]
          } yield policiesStatuses match {
            case Failure(exception) => throw exception
            case Success(statuses) => statuses
          }
        }
      }
    }
  }

  @ApiOperation(value = "Find a policy context from its id.",
    notes = "Find a policy context from its id.",
    httpMethod = "GET",
    response = classOf[PolicyStatusModel])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id",
      value = "id of the policy",
      dataType = "string",
      required = true,
      paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)
  ))
  def find(user: Option[LoggedUser]): Route = {
    path(HttpConstant.PolicyContextPath / Segment) { (id) =>
      get {
        complete {
          val statusActor = actors(AkkaConstant.StatusActorName)
          for {
            policyStatus <- (statusActor ? new FindById(id)).mapTo[ResponseStatus]
          } yield policyStatus match {
            case ResponseStatus(Failure(exception)) => throw exception
            case ResponseStatus(Success(policy)) => policy
          }
        }
      }
    }
  }

  @ApiOperation(value = "Delete all policy contexts",
    notes = "Delete all policy contexts",
    httpMethod = "DELETE")
  @ApiResponses(
    Array(new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)))
  def deleteAll(user: Option[LoggedUser]): Route = {
    path(HttpConstant.PolicyContextPath) {
      delete {
        complete {
          val statusActor = actors(AkkaConstant.StatusActorName)
          for {
            responseCode <- (statusActor ? DeleteAll).mapTo[ResponseDelete]
          } yield responseCode match {
            case ResponseDelete(Failure(exception)) => throw exception
            case ResponseDelete(Success(_)) => StatusCodes.OK
          }
        }
      }
    }
  }

  @ApiOperation(value = "Delete a policy contexts by its id",
    notes = "Delete a policy contexts by its id",
    httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id",
      value = "id of the policy",
      dataType = "string",
      required = true,
      paramType = "path")
  ))
  @ApiResponses(
    Array(new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)))
  def deleteById(user: Option[LoggedUser]): Route = {
    path(HttpConstant.PolicyContextPath / Segment) { (id) =>
      delete {
        complete {
          val statusActor = actors(AkkaConstant.StatusActorName)
          for {
            responseDelete <- (statusActor ? Delete(id)).mapTo[ResponseDelete]
          } yield responseDelete match {
            case ResponseDelete(Failure(exception)) => throw exception
            case ResponseDelete(Success(_)) => StatusCodes.OK
          }
        }
      }
    }
  }

  @ApiOperation(value = "Updates a policy status.",
    notes = "Updates a policy status.",
    httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "policy status",
      value = "policy json",
      dataType = "PolicyStatusModel",
      required = true,
      paramType = "body")))
  def update(user: Option[LoggedUser]): Route = {
    path(HttpConstant.PolicyContextPath) {
      put {
        entity(as[PolicyStatusModel]) { policyStatus =>
          complete {
            val statusActor = actors(AkkaConstant.StatusActorName)
            for {
              response <- (statusActor ? Update(policyStatus)).mapTo[ResponseStatus]
            } yield response match {
              case ResponseStatus(Success(status)) => HttpResponse(StatusCodes.Created)
              case ResponseStatus(Failure(ex)) =>
                log.error("Can't update policy", ex)
                throw new ServingCoreException(ErrorModel.toString(
                  ErrorModel(ErrorModel.CodeErrorUpdatingPolicy, "Can't update policy")
                ))
            }
          }
        }
      }
    }
  }

  @ApiOperation(value = "Creates a policy, the status context and launch the policy created.",
    notes = "Returns the result",
    httpMethod = "POST",
    response = classOf[PolicyResult])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "policy",
      value = "policy json",
      dataType = "AggregationPoliciesModel",
      required = true,
      paramType = "body")))
  @ApiResponses(
    Array(new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)))
  def create(user: Option[LoggedUser]): Route = {
    path(HttpConstant.PolicyContextPath) {
      post {
        entity(as[PolicyModel]) { inputPolicy =>
          complete {
            val fragmentActor = actors.getOrElse(AkkaConstant.FragmentActorName, throw new ServingCoreException
            (ErrorModel.toString(ErrorModel(ErrorModel.CodeUnknown, s"Error getting fragmentActor"))))
            for {
              parsedP <- (fragmentActor ? FragmentActor.PolicyWithFragments(inputPolicy)).mapTo[ResponsePolicy]
            } yield parsedP match {
              case ResponsePolicy(Failure(exception)) =>
                throw exception
              case ResponsePolicy(Success(policyParsed)) =>
                PolicyValidator.validateDto(policyParsed)
                for {
                  policyResponseTry <- (supervisor ? Launch(policyParsed)).mapTo[Try[PolicyModel]]
                } yield {
                  policyResponseTry match {
                    case Success(policy) =>
                      val inputs = FragmentsHelper.populateFragmentFromPolicy(policy, FragmentType.input)
                      val outputs = FragmentsHelper.populateFragmentFromPolicy(policy, FragmentType.output)
                      createFragments(fragmentActor, outputs.toList ::: inputs.toList)
                      PolicyResult(policy.id.getOrElse(""), policy.name)
                    case Failure(ex: Throwable) =>
                      log.error("Can't create policy", ex)
                      throw new ServingCoreException(ErrorModel.toString(
                        ErrorModel(ErrorModel.CodeErrorCreatingPolicy, "Can't create policy")
                      ))
                  }
                }
            }
          }
        }
      }
    }
  }

  // XXX Protected methods

  protected def createFragments(fragmentActor: ActorRef, fragments: Seq[FragmentElementModel]): Unit =
    fragments.foreach(fragment => fragmentActor ! FragmentActor.Create(fragment))
}

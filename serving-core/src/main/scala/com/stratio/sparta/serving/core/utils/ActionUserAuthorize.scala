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
package com.stratio.sparta.serving.core.utils

import com.stratio.sparta.security.{Action, SpartaSecurityManager}
import com.stratio.sparta.serving.core.models.dto.LoggedUser
import akka.actor.Actor
import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.serving.core.helpers.SecurityManagerHelper._

trait ActionUserAuthorize extends Actor with SLF4JLogging {

  def securityActionAuthorizer[T](user: Option[LoggedUser], actions : Map[String, Action])(actionFunction: => T)(
    implicit secManagerOpt: Option[SpartaSecurityManager]
  ): Unit =
    (secManagerOpt, user) match {
      case (Some(secManager), Some(userLogged)) =>

        val rejectedActions = actions filterNot {
          case (resource, action) => secManager.authorize(userLogged.id, resource, action)
        }

        sender ! {
          rejectedActions.headOption map { _ => // There are rejected actions.
            log.debug(s"Not authorized: Actions: $actions \t Rejected: ${rejectedActions.head}")
            Right(errorResponseAuthorization(userLogged.id, actions.head._1))
          } getOrElse { // All actions've been accepted.
            log.debug(s"Authorized! Actions: $actions")
            Left(actionFunction)
          }
        }

      case (Some(_), None) => sender ! Right(errorNoUserFound(actions.values.toSeq))
      case (None, _) => sender ! Left(actionFunction)
    }

}

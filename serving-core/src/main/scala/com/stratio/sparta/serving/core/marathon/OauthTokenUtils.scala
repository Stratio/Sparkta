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

package com.stratio.sparta.serving.core.marathon

import java.net.HttpCookie

import akka.event.slf4j.SLF4JLogging
import com.stratio.tikitakka.common.util.ConfigComponent

import scala.util.{Failure, Success, Try}
import scalaj.http.{Http, HttpResponse}

trait OauthTokenUtils extends SLF4JLogging {

  import OauthTokenUtils._

  lazy val ssoUri = ConfigComponent.getString(ssoUriField).getOrElse(throw new Exception("SSO Uri not defined"))
  lazy val username = ConfigComponent.getString(usernameField).getOrElse(throw new Exception("username not defined"))
  lazy val password = ConfigComponent.getString(passwordField).getOrElse(throw new Exception("password not defined"))
  lazy val clientId = ConfigComponent.getString(clientIdField).getOrElse(throw new Exception("clientId not defined"))
  lazy val redirectUri = ConfigComponent.getString(redirectUriField)
    .getOrElse(throw new Exception("redirectUri not defined"))
  val defaultRetries = 5
  val maxRetries = Try(ConfigComponent.getInt(retriesField).get).getOrElse(defaultRetries)
  val dcosAuthCookieName = "dcos-acs-auth-cookie"

  def getToken: HttpCookie = {
      currentToken match {
      case Some(t) if !t.hasExpired => t
      case _ => Try(retryGetToken(1)(retrieveTokenFromSSO)) match {
        case Success(token) =>
          currentToken = Option(token)
          token
        case Failure(ex: Throwable) =>
          throw new Exception(s"ERROR: ${ex.getMessage}. For further information, please consult the application log")
      }
    }
  }

  var currentToken: Option[HttpCookie] = None

  def retryGetToken(numberCurrentRetries: Int)(getTokenFn: => Try[Seq[HttpCookie]]): HttpCookie =
    if(numberCurrentRetries <= maxRetries){
      retrieveTokenFromSSO match {
        case Success(tokenCookie: Seq[HttpCookie]) => {
          val dcosCookie = tokenCookie.filter(cookie =>
            cookie.getName.equalsIgnoreCase(dcosAuthCookieName))
          if (dcosCookie.nonEmpty) {
            log.debug(s"""Marathon Token "$dcosAuthCookieName" correctly retrieved """ +
              s"at retry attempt n. $numberCurrentRetries")
            dcosCookie.head
          }
          else {
            log.debug(s"Retry attempt n. $numberCurrentRetries :" +
              s"Error trying to recover the Oauth token: cookie $dcosAuthCookieName not found")
            retryGetToken(numberCurrentRetries + 1)(getTokenFn)
          }
        }
        case Failure(ex: Throwable) =>
          log.debug(s"Retry attempt n. $numberCurrentRetries :" +
            s" Error trying to recover Oauth token: ${ex.getMessage}")
          retryGetToken(numberCurrentRetries + 1)(getTokenFn)
        case _ =>
          log.debug(s"Retry attempt n. $numberCurrentRetries :" +
            s" Token not found in last response")
          retryGetToken(numberCurrentRetries + 1)(getTokenFn)
      }
    } else {
      throw new Exception(s"It was not possible to recover the Oauth token " +
        s"$dcosAuthCookieName after $maxRetries retries")
    }

    def retrieveTokenFromSSO: Try[Seq[HttpCookie]] =
      Try {
      // First request (AUTHORIZE)
      val authRequest = s"$ssoUri/oauth2.0/authorize?redirect_uri=$redirectUri&client_id=$clientId"
      log.debug(s"1. Request to : $authRequest")
      val authResponse = Http(authRequest).asString
      val JSESSIONIDCookie = getCookie(authResponse)

      // Second request (Redirect to LOGIN)
      val redirectToLogin = extractRedirectUri(authResponse)
      log.debug(s"2. Redirect to : $redirectToLogin with JSESSIONID cookie")
      val postFormUri = Http(redirectToLogin).cookies(JSESSIONIDCookie).asString
      val (lt, execution) = extractLTAndExecution(postFormUri.body)

      // Third request (POST)
      val loginPostUri = s"$ssoUri/login?service=$ssoUri/oauth2.0/callbackAuthorize"
      log.debug(s"3. Request to $loginPostUri with JSESSIONID cookie")
      val loginResponse = Http(loginPostUri)
        .cookies(JSESSIONIDCookie)
        .postForm(createLoginForm(lt, execution))
        .asString

      val CASPRIVACY_AND_TGC_COOKIES = getCookie(loginResponse)

      // Fourth request (Redirect from POST)
      val callbackUri = extractRedirectUri(loginResponse)
      log.debug(s"4. Redirect to : $callbackUri with JSESSIONID, CASPRIVACY and TGC cookies")
      val ticketResponse = Http(callbackUri).cookies(CASPRIVACY_AND_TGC_COOKIES union JSESSIONIDCookie).asString

      // Fifth request (Redirect with Ticket)
      val clientRedirectUri = extractRedirectUri(ticketResponse)
      log.debug(s"5. Redirect to : $clientRedirectUri with JSESSIONID, CASPRIVACY and TGC cookies")
      val tokenResponse =
        Http(clientRedirectUri).cookies(CASPRIVACY_AND_TGC_COOKIES union JSESSIONIDCookie).asString

      getCookie(tokenResponse)
    }

  def extractRedirectUri(response: HttpResponse[String]): String =
    response.headers.get("Location").get.head

  def extractLTAndExecution(body: String): (String, String) = {
    val ltLEftMAtch = "name=\"lt\" value=\""
    val lt1 = body.indexOf(ltLEftMAtch)
    val prelt = body.substring(lt1 + ltLEftMAtch.length)
    val lt = prelt.substring(0, prelt.indexOf("\" />")).trim

    val executionLEftMAtch = "name=\"execution\" value=\""
    val execution1 = body.indexOf(executionLEftMAtch)
    val execution = body.substring(execution1 + executionLEftMAtch.length).split("\"")(0)

    (lt, execution)
  }

  def createLoginForm(lt: String, execution: String): Seq[(String, String)] =
    Seq(
      "lt" -> lt,
      "_eventId" -> "submit",
      "execution" -> execution,
      "submit" -> "LOGIN",
      "username" -> username,
      "password" -> password
    )

  def getCookie(response: HttpResponse[String]): Seq[HttpCookie] = {
    response.headers.get("Set-Cookie") match {
      case Some(cookies) =>
        cookies.map { cookie =>
          val cookieFields = cookie.split(";")(0).split("=")
          new HttpCookie(cookieFields(0), cookieFields(1))
        }.distinct
      case None => Seq.empty[HttpCookie]
    }
  }
}

object OauthTokenUtils {

  val ssoUriField = "sparta.marathon.sso.uri"
  val usernameField = "sparta.marathon.sso.username"
  val passwordField = "sparta.marathon.sso.password"
  val clientIdField = "sparta.marathon.sso.clientId"
  val redirectUriField = "sparta.marathon.sso.redirectUri"
  val retriesField = "sparta.marathon.sso.retries"
}


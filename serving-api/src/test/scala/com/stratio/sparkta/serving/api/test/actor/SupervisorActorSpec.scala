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

package com.stratio.sparkta.driver.test.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.stratio.sparkta.driver.models.StreamingContextStatusEnum._
import com.stratio.sparkta.driver.models._
import com.stratio.sparkta.driver.service.StreamingContextService
import com.stratio.sparkta.sdk.exception.MockException
import com.stratio.sparkta.serving.api.actor._
import com.stratio.sparkta.serving.api.exception.ServingApiException
import com.typesafe.config.ConfigFactory
import org.apache.spark.streaming.StreamingContext
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration.DurationInt

@RunWith(classOf[JUnitRunner])
class SupervisorActorSpec
  extends TestKit(ActorSystem("SupervisorActorSpec",
    ConfigFactory.parseString(SupervisorActorSpec.config)))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfter with BeforeAndAfterAll with MockitoSugar {

  var streamingContextService: Option[StreamingContextService] = None
  val jobServerActor = None

  before {
    streamingContextService = Some(mock[StreamingContextService])
    val ssc = mock[StreamingContext]
    doNothing().when(ssc).start()
    when(streamingContextService.get.createStreamingContext(any[AggregationPoliciesModel], any[Option[ActorRef]]))
      .thenReturn(ssc)
  }

  after {
    streamingContextService = None
  }

  override protected def afterAll(): Unit = {
    shutdown(system)
  }

  "An SupervisorActor" should {
    "Init a StreamingContextActor and DriverException error is thrown" in {
      val supervisorRef = createSupervisorActor(jobServerActor)
      val errorMessage = "An error occurred"


      when(streamingContextService.get.createStreamingContext(any[AggregationPoliciesModel], any[Option[ActorRef]]))
        .thenThrow(new ServingApiException(errorMessage))

      within(5000 millis) {
        supervisorRef ! new CreateContext(createPolicyConfiguration("test-1"))
        expectNoMsg
      }

      within(5000 millis) {
        supervisorRef ! new GetContextStatus("test-1")
        expectMsg(new StreamingContextStatus("test-1", ConfigurationError, Some(errorMessage)))
      }
    }
    "Init a StreamingContextActor and any unexpected error is thrown" in {
      val supervisorRef = createSupervisorActor(jobServerActor)
      val mockErrorMessage: String = "A mock error occurred"

      when(streamingContextService.get.createStreamingContext(any[AggregationPoliciesModel], any[Option[ActorRef]]))
        .thenThrow(new MockException(mockErrorMessage))
      within(5000 millis) {
        supervisorRef ! new CreateContext(createPolicyConfiguration("test-1"))
        expectNoMsg
      }

      within(5000 millis) {
        supervisorRef ! new GetContextStatus("test-1")
        expectMsg(new StreamingContextStatus("test-1", Error, Some(mockErrorMessage)))
      }
    }
    //TODO test when creating a streamingContextActor unexpected error occurs
    "Init a StreamingContextActor" in {

      val supervisorRef = createSupervisorActor(jobServerActor)

      within(5000 millis) {
        supervisorRef ! new CreateContext(createPolicyConfiguration("test-1"))
        expectNoMsg
      }

      within(5000 millis) {
        supervisorRef ! new GetContextStatus("test-1")
        expectMsg(new StreamingContextStatus("test-1", Initialized, None))
      }
    }
    "Get a context status for a created context" in {
      val supervisorRef = createSupervisorActor(jobServerActor)

      within(5000 millis) {
        supervisorRef ! new CreateContext(createPolicyConfiguration("test-1"))
        expectNoMsg
      }

      within(5000 millis) {
        supervisorRef ! new GetContextStatus("test-1")
        expectMsg(new StreamingContextStatus("test-1", Initialized, None))
      }
    }
    "Delete a previously created context" in {

      val supervisorRef = createSupervisorActor(jobServerActor)

      within(5000 millis) {
        supervisorRef ! new CreateContext(createPolicyConfiguration("test-1"))
        expectNoMsg
      }

      within(5000 millis) {
        supervisorRef ! new DeleteContext("test-1")
        expectMsg(new StreamingContextStatus("test-1", Removed, None))
      }
    }
    "Get all context statuses" in {

      val supervisorRef = createSupervisorActor(jobServerActor)

      within(5000 millis) {

        supervisorRef ! new CreateContext(createPolicyConfiguration("test-1"))
        expectNoMsg

        supervisorRef ! new CreateContext(createPolicyConfiguration("test-2"))
        expectNoMsg

        supervisorRef ! new CreateContext(createPolicyConfiguration("test-3"))
        expectNoMsg

        supervisorRef ! GetAllContextStatus
        val contextData = receiveWhile(5000 millis) {
          case msg: List[StreamingContextStatus] =>
            msg
        }
        contextData.size should be(1)

        contextData.map(d => {
          d.size should be(3)
          val names = d.map(_.name)
          names should contain("test-1")
          names should contain("test-2")
          names should contain("test-3")
        })
      }
    }
  }

  private def createSupervisorActor(jobServerActor: Option[ActorRef]): ActorRef = {
    system.actorOf(Props(new StreamingActor(streamingContextService.get, jobServerActor)))
  }

  private def createPolicyConfiguration(name: String): AggregationPoliciesModel = {
    val policyConfiguration = mock[AggregationPoliciesModel]
    when(policyConfiguration.name).thenReturn(name)
    policyConfiguration
  }
}

object SupervisorActorSpec {

  val config = """
               akka {
                 loglevel ="OFF"
               }
               """
}

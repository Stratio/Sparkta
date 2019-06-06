/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.serving.api.service.http

import akka.actor.ActorRef
import akka.testkit.TestProbe
import com.stratio.sparta.core.exception.MockException
import com.stratio.sparta.serving.api.actor.TemplateActor._
import com.stratio.sparta.serving.api.constants.HttpConstant
import com.stratio.sparta.serving.core.config.{SpartaConfig, SpartaConfigFactory}
import com.stratio.sparta.serving.core.constants.AkkaConstant
import com.stratio.sparta.serving.core.models.authorization.{GosecUser, GosecUserConstants, LoggedUser}
import com.stratio.sparta.serving.core.models.workflow.TemplateElement
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import spray.http.StatusCodes

import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class TemplateHttpServiceTest extends WordSpec
  with TemplateHttpService
  with HttpServiceBaseTest {

  val policyTestProbe = TestProbe()
  val fragmentTestProbe = TestProbe()
  val dummyUser = Some(GosecUserConstants.AnonymousUser)
  val rootUser = Some(GosecUser("1234", "root", "dummyMail", "0", Seq.empty, Seq.empty))

  override implicit val actors: Map[String, ActorRef] = Map(
    AkkaConstant.WorkflowActorName -> policyTestProbe.ref,
    AkkaConstant.TemplateActorName -> fragmentTestProbe.ref
  )
  override val supervisor: ActorRef = testProbe.ref

  override def beforeEach(): Unit = {
    SpartaConfig.getSpartaConfig(Option(localConfig))
    SpartaConfig.getZookeeperConfig(Option(localConfig))
    SpartaConfig.getDetailConfig(Option(localConfig))
  }

  "TemplateHttpService.findByTypeAndId" should {
    "find a fragment" in {
      startAutopilot(Left(Success(getFragmentModel())))
      Get(s"/${HttpConstant.TemplatePath}/input/id/fragmentId") ~> routes(rootUser) ~> check {
        testProbe.expectMsgType[FindByTypeAndId]
        responseAs[TemplateElement] should equal((getFragmentModel()))
      }
    }
    "return a 500 if there was any error" in {
      startAutopilot(Left(Failure(new MockException())))
      Get(s"/${HttpConstant.TemplatePath}/input/id/fragmentId") ~> routes(dummyUser) ~> check {
        testProbe.expectMsgType[FindByTypeAndId]
        status should be(StatusCodes.InternalServerError)
      }
    }
  }

  "TemplateHttpService.findByTypeAndName" should {
    "find a fragment" in {
      startAutopilot(Left(Success(getFragmentModel())))
      Get(s"/${HttpConstant.TemplatePath}/input/name/fragment") ~> routes(rootUser) ~> check {
        testProbe.expectMsgType[FindByTypeAndName]
        responseAs[TemplateElement] should equal(getFragmentModel())
      }
    }
    "return a 500 if there was any error" in {
      startAutopilot(Left(Failure(new MockException())))
      Get(s"/${HttpConstant.TemplatePath}/input/name/fragment") ~> routes(dummyUser) ~> check {
        testProbe.expectMsgType[FindByTypeAndName]
        status should be(StatusCodes.InternalServerError)
      }
    }
  }

  "TemplateHttpService.findAllByType" should {
    "find all fragments" in {
      startAutopilot(Left(Success(Seq(getFragmentModel()))))
      Get(s"/${HttpConstant.TemplatePath}/input") ~> routes(rootUser) ~> check {
        testProbe.expectMsgType[FindByType]
        responseAs[Seq[TemplateElement]] should equal(Seq(getFragmentModel()))
      }
    }
    "return a 500 if there was any error" in {
      startAutopilot(Left(Failure(new MockException())))
      Get(s"/${HttpConstant.TemplatePath}/input") ~> routes(dummyUser) ~> check {
        testProbe.expectMsgType[FindByType]
        status should be(StatusCodes.InternalServerError)
      }
    }
  }

  "TemplateHttpService.findAll" should {
    "find all fragments" in {
      startAutopilot(Left(Success(Seq(getFragmentModel()))))
      Get(s"/${HttpConstant.TemplatePath}") ~> routes(rootUser) ~> check {
        testProbe.expectMsgType[FindAllTemplates]
        responseAs[Seq[TemplateElement]] should equal(Seq(getFragmentModel()))
      }
    }
    "return a 500 if there was any error" in {
      startAutopilot(Left(Failure(new MockException())))
      Get(s"/${HttpConstant.TemplatePath}") ~> routes(dummyUser) ~> check {
        testProbe.expectMsgType[FindAllTemplates]
        status should be(StatusCodes.InternalServerError)
      }
    }
  }

  "TemplateHttpService.create" should {
    "return the fragment that was created" in {
      startAutopilot(Left(Success(getFragmentModel())))
      Post(s"/${HttpConstant.TemplatePath}", getFragmentModel()) ~> routes(rootUser) ~> check {
        testProbe.expectMsgType[CreateTemplate]
        responseAs[TemplateElement] should equal(getFragmentModel())
      }
    }
    "return a 500 if there was any error" in {
      startAutopilot(Left(Failure(new MockException())))
      Post(s"/${HttpConstant.TemplatePath}", getFragmentModel()) ~> routes(dummyUser) ~> check {
        testProbe.expectMsgType[CreateTemplate]
        status should be(StatusCodes.InternalServerError)
      }
    }
  }

  "TemplateHttpService.update" should {
    "return the fragment that was updated" in {
      startAutopilot(Left(Success(getFragmentModel())))
      Put(s"/${HttpConstant.TemplatePath}", getFragmentModel()) ~> routes(rootUser) ~> check {
        testProbe.expectMsgType[Update]
        status should be(StatusCodes.OK)
      }
    }
    "return a 500 if there was any error" in {
      startAutopilot(Left(Failure(new MockException())))
      Put(s"/${HttpConstant.TemplatePath}", getFragmentModel()) ~> routes(dummyUser) ~> check {
        testProbe.expectMsgType[Update]
        status should be(StatusCodes.InternalServerError)
      }
    }
  }

  "TemplateHttpService.deleteByTypeAndId" should {
    "return an OK because the fragment was deleted" in {
      startAutopilot(Left(Success(None)))
      Delete(s"/${HttpConstant.TemplatePath}/input/id/fragmentId") ~> routes(rootUser) ~> check {
        testProbe.expectMsgType[DeleteByTypeAndId]
        status should be(StatusCodes.OK)
      }
    }
    "return a 500 if there was any error" in {
      startAutopilot(Left(Failure(new MockException())))
      Delete(s"/${HttpConstant.TemplatePath}/input/id/fragmentId") ~> routes(rootUser) ~> check {
        testProbe.expectMsgType[DeleteByTypeAndId]
        status should be(StatusCodes.InternalServerError)
      }
    }
  }

  "TemplateHttpService.deleteByTypeAndName" should {
    "return an OK because the fragment was deleted" in {
      startAutopilot(Left(Success(None)))
      Delete(s"/${HttpConstant.TemplatePath}/input/name/fragmentName") ~> routes(rootUser) ~> check {
        testProbe.expectMsgType[DeleteByTypeAndName]
        status should be(StatusCodes.OK)
      }
    }
    "return a 500 if there was any error" in {
      startAutopilot(Left(Failure(new MockException())))
      Delete(s"/${HttpConstant.TemplatePath}/input/name/fragmentName") ~> routes(rootUser) ~> check {
        testProbe.expectMsgType[DeleteByTypeAndName]
        status should be(StatusCodes.InternalServerError)
      }
    }
  }
}

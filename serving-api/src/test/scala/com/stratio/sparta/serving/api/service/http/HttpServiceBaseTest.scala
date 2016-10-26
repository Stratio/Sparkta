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

import akka.actor.{ActorRef, ActorRefFactory}
import akka.testkit.TestActor.AutoPilot
import akka.testkit.{TestActor, TestProbe}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import spray.testkit.ScalatestRouteTest
import com.stratio.sparta.sdk._
import com.stratio.sparta.serving.core.models._
import com.stratio.sparta.serving.core.policy.status.PolicyStatusEnum
import com.typesafe.config.ConfigFactory

/**
 * Common operations for http service specs. All of them must extend from this class.
 */
trait HttpServiceBaseTest extends WordSpec
  with Matchers
  with BeforeAndAfterEach
  with ScalatestRouteTest
  with SpartaSerializer {

  val testProbe: TestProbe = TestProbe()

  def actorRefFactory: ActorRefFactory = system

  val granularity = 30000
  val interval = 60000

  val localConfig = ConfigFactory.parseString(
    """
      |sparta{
      |   config {
      |     executionMode = local
      |   }
      |
      |   local {
      |    spark.app.name = SPARTA
      |    spark.master = "local[*]"
      |    spark.executor.memory = 1024m
      |    spark.app.name = SPARTA
      |    spark.sql.parquet.binaryAsString = true
      |    spark.streaming.concurrentJobs = 1
      |    #spark.metrics.conf = /opt/sds/sparta/benchmark/src/main/resources/metrics.properties
      |  }
      |}
    """.stripMargin)

  // XXX Protected methods.

  protected def getTemplateModel(): TemplateModel =
    new TemplateModel("template", "input", Map(), Map(), Seq())

  protected def getFragmentModel(id: Option[String]): FragmentElementModel =
    new FragmentElementModel(id, "input", "name", "description", "shortDescription",
      new PolicyElementModel("name", "input", Map()))

  protected def getFragmentModel(): FragmentElementModel =
    getFragmentModel(None)

  protected def getPolicyStatusModel(): PolicyStatusModel =
    new PolicyStatusModel("id", PolicyStatusEnum.Launched)

  protected def getPolicyWithStatus(): PolicyWithStatus =
    PolicyWithStatus(PolicyStatusEnum.Launched, getPolicyModel())

  protected def getPolicyModel(): AggregationPoliciesModel = {
    val rawData = new RawDataModel
    val outputFieldModel1 = OutputFieldsModel("out1")
    val outputFieldModel2 = OutputFieldsModel("out2")

    val transformations =
      Seq(TransformationsModel("Morphlines", 0, Input.RawDataKey, Seq(outputFieldModel1, outputFieldModel2), Map()))
    val dimensionModel = Seq(DimensionModel(
      "dimensionName",
      "field1",
      DimensionType.IdentityName,
      DimensionType.DefaultDimensionClass,
      configuration = Some(Map()))
    )
    val writerModel = WriterModel(Seq("mongo"))
    val operators = Seq(OperatorModel("Count", "countoperator", Map()))
    val cubes = Seq(CubeModel("cube1", dimensionModel, operators, writerModel))
    val outputs = Seq(PolicyElementModel("mongo", "MongoDb", Map()))
    val input = Some(PolicyElementModel("kafka", "Kafka", Map()))
    val policy = AggregationPoliciesModel(id = Option("id"),
      version = None,
      storageLevel = AggregationPoliciesModel.storageDefaultValue,
      name = "testpolicy",
      description = "whatever",
      sparkStreamingWindow = AggregationPoliciesModel.sparkStreamingWindow,
      checkpointPath = Option("test/test"),
      rawData,
      transformations,
      Seq(),
      cubes,
      input,
      outputs,
      Seq(),
      userPluginsJars = Seq(),
      remember = None,
      sparkConf = Seq(),
      initSqlSentences = Seq()
    )
    policy
  }

  /**
   * Starts and actor used to reply messages though the akka system. By default it can send a message to reply and it
   * will be use a default TestProbe. Also, it is possible to send a custom autopilot with a specific logic.
   *
   * @param message          that will be sent to the actor.
   * @param currentTestProbe with the [TestProbe] to use.
   * @param autopilot        is an actor that contains the logic that will be used when a message arrives.
   */
  protected def startAutopilot(message: Any,
                               currentTestProbe: TestProbe = this.testProbe,
                               autopilot: Option[AutoPilot] = None): Unit = {
    currentTestProbe.setAutoPilot(
      autopilot.getOrElse(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any): TestActor.AutoPilot =
          msg match {
            case x =>
              sender ! message
              TestActor.NoAutoPilot
          }
      })
    )
  }
}

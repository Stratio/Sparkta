/**
  * Copyright (C) 2016 Stratio (http://stratio.com)
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

package com.stratio.sparkta.serving.core.models.test

import com.stratio.sparkta.serving.core.helpers._
import com.stratio.sparkta.serving.core.models.
{CommonCheckpointModel, OperatorModel, WorkflowCubeModel, WorkflowDimensionModel}
import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ParseWorkflowToCommonModelTest extends WordSpec with Matchers {

  val computeLast = "60000"
  val checkpointModel = CommonCheckpointModel("minute", "2h", Some("30000"), Some(computeLast))
  val operators = Seq(OperatorModel("Count", "countoperator", Map()))
  val workflowDimensionsModel = Seq(WorkflowDimensionModel(
    "minute",
    "field1",
    "2h",
    "DateType",
    Some(computeLast.toString),
    Some(Map())
  ))

  val workflowDimensionModel = WorkflowDimensionModel(
    "dimensionName",
    "field1",
    "2h",
    "DateType",
    Option(computeLast.toString),
    Some(Map())
  )

  val workflowDimensionsModel2 = Seq(WorkflowDimensionModel(
    "minute",
    "field1",
    "2h",
    "DateType",
    None,
    Some(Map())
  ), WorkflowDimensionModel(
    "dimension1",
    "field1",
    "5h",
    "DateType",
    Some(computeLast.toString),
    Some(Map())
  ))
  val workflowDimensionModel3 = Seq(WorkflowDimensionModel(
    "dimensionName",
    "field1",
    "2h",
    "DateType",
    None,
    Some(Map()))
  )
  val workflowDimensionsModel4 = Seq(WorkflowDimensionModel(
    "minute",
    "field1",
    "2h",
    "DateType",
    None,
    Some(Map())
  ), WorkflowDimensionModel(
    "dimension1",
    "field1",
    "2h",
    "DateType",
    None,
    Some(Map())
  ))
  val workflowCubeModel = WorkflowCubeModel("cube1",
    "30000",
    workflowDimensionsModel,
    operators: Seq[OperatorModel])

  val workflowCubeModel2 = WorkflowCubeModel("cube-test",
    "30000",
    workflowDimensionsModel2,
    operators: Seq[OperatorModel])

  val workflowCubeModel3 = WorkflowCubeModel("cube-test",
    "30000",
    workflowDimensionModel3,
    operators: Seq[OperatorModel])

  val workflowCubeModel4 = WorkflowCubeModel("cube-test",
    "30000",
    workflowDimensionsModel4,
    operators: Seq[OperatorModel])


  val workflowCubesModel = Seq(workflowCubeModel)
  val workflowCubesModel2 = Seq(workflowCubeModel)

  /*
   * Test for createCheckpoint
   */

  val workflowCubeModelcp = WorkflowCubeModel("cube1",
    "30000",
    workflowDimensionsModel,
    operators: Seq[OperatorModel])

  "AggregationPolicySpec" should {

    "findWorkflowTimeDimension should return the timeDimensionName minute" in {
      val res = ParseWorkflowToCommonModel.findWorkflowTimeDimension(workflowCubeModel)
      res should be (Some(List("minute", "2h", s"$computeLast")))
    }

    "findWorkflowTimeDimension should return the timeDimensionName fake" in {
      val res = ParseWorkflowToCommonModel.findWorkflowTimeDimension(workflowCubeModel2)
      res should be (Some(List("dimension1", "5h", s"$computeLast")))
    }
    "findWorkflowTimeDimension should return the timeDimensionName of the dimension with computeLast" in {
      val res = ParseWorkflowToCommonModel.findWorkflowTimeDimension(workflowCubeModel3)
      res should be(None)
    }

    "findWorkflowTimeDimension should return the timeDimensionName of two dimensions " +
      "with the dimension with computeLast" in {
      val res = ParseWorkflowToCommonModel.findWorkflowTimeDimension(workflowCubeModel4)
      res should be(None)
    }

    "createCheckpoint should create a commonCheckpointModel"  in {
      val res = ParseWorkflowToCommonModel.createCheckpoint(workflowCubeModelcp)
      res should be(checkpointModel)
    }
  }

}

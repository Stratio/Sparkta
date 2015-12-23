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

package com.stratio.sparkta.plugin.operator.accumulator

import com.stratio.sparkta.sdk.{InputFieldsValues, InputFields, Operator}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

@RunWith(classOf[JUnitRunner])
class AccumulatorOperatorTest extends WordSpec with Matchers {

  "Accumulator operator" should {

    "processMap must be " in {
      val inputField = new AccumulatorOperator("accumulator", Map())
      inputField.processMap(InputFieldsValues(Map("field1" -> 1, "field2" -> 2))) should be(None)

      val inputFields2 = new AccumulatorOperator("accumulator", Map("inputField" -> "field1"))
      inputFields2.processMap(InputFieldsValues(Map("field3" -> 1, "field2" -> 2))) should be(None)

      val inputFields3 = new AccumulatorOperator("accumulator", Map("inputField" -> "field1"))
      inputFields3.processMap(InputFieldsValues(Map("field1" -> 1, "field2" -> 2))) should be(Some(1))

      val inputFields4 = new AccumulatorOperator("accumulator",
        Map("inputField" -> "field1", "filters" -> "[{\"field\":\"field1\", \"type\": \"<\", \"value\":2}]"))
      inputFields4.processMap(InputFieldsValues(Map("field1" -> 1, "field2" -> 2))) should be(Some(1L))

      val inputFields5 = new AccumulatorOperator("accumulator",
        Map("inputField" -> "field1", "filters" -> "[{\"field\":\"field1\", \"type\": \">\", \"value\":\"2\"}]"))
      inputFields5.processMap(InputFieldsValues(Map("field1" -> 1, "field2" -> 2))) should be(None)

      val inputFields6 = new AccumulatorOperator("accumulator",
        Map("inputField" -> "field1", "filters" -> {"[{\"field\":\"field1\", \"type\": \"<\", \"value\":\"2\"}," +
          "{\"field\":\"field2\", \"type\": \"<\", \"value\":\"2\"}]"}))
      inputFields6.processMap(InputFieldsValues(Map("field1" -> 1, "field2" -> 2))) should be(None)
    }

    "processReduce must be " in {
      val inputFields = new AccumulatorOperator("accumulator", Map())
      inputFields.processReduce(Seq()) should be(Some(Seq()))

      val inputFields2 = new AccumulatorOperator("accumulator", Map())
      inputFields2.processReduce(Seq(Some(1), Some(1))) should be(Some(Seq("1", "1")))

      val inputFields3 = new AccumulatorOperator("accumulator", Map())
      inputFields3.processReduce(Seq(Some("a"), Some("b"))) should be(Some(Seq("a", "b")))

    }

    "associative process must be " in {
      val inputFields = new AccumulatorOperator("accumulator", Map())
      val resultInput = Seq((Operator.OldValuesKey, Some(Seq(1L))),
        (Operator.NewValuesKey, Some(Seq(2L))),
        (Operator.NewValuesKey, None))
      inputFields.associativity(resultInput) should be(Some(Seq("1", "2")))

      val inputFields2 = new AccumulatorOperator("accumulator", Map("typeOp" -> "arraydouble"))
      val resultInput2 = Seq((Operator.OldValuesKey, Some(Seq(1))),
        (Operator.NewValuesKey, Some(Seq(3))))
      inputFields2.associativity(resultInput2) should be(Some(Seq(1d, 3d)))

      val inputFields3 = new AccumulatorOperator("accumulator", Map("typeOp" -> null))
      val resultInput3 = Seq((Operator.OldValuesKey, Some(Seq(1))),
        (Operator.NewValuesKey, Some(Seq(1))))
      inputFields3.associativity(resultInput3) should be(Some(Seq("1", "1")))

    }
  }
}

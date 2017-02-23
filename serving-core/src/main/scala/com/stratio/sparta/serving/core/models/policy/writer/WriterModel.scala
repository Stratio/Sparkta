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

package com.stratio.sparta.serving.core.models.policy.writer

import com.stratio.sparta.sdk.pipeline.output.SaveModeEnum

case class WriterModel(outputs: Seq[String] = Seq.empty[String],
                       dateType: Option[String] = None,
                       autoCalculatedFields: Seq[AutoCalculatedFieldModel] = Seq.empty[AutoCalculatedFieldModel],
                       saveMode: SaveModeEnum.Value = SaveModeEnum.Append,
                       tableName: Option[String] = None)

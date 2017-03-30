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

package com.stratio.sparta.serving.core.models.enumerators

/**
 * Possible states that a policy could be when it was run.
 *
 * Launched: Sparta performs a spark-submit to the cluster.
 * Starting: SpartaJob tries to start the job.
 * Started: if the job was successfully started and the receiver is running.
 * Failed: if the lifecycle fails.
 * Stopping: Sparta sends a stop signal to the job to stop it gracefully.
 * Stopped: the job is stopped.
 */
object PolicyStatusEnum extends Enumeration {

  type status = Value

  val Launched = Value("Launched")
  val Starting = Value("Starting")
  val Started = Value("Started")
  val Failed = Value("Failed")
  val Stopping = Value("Stopping")
  val Stopped = Value("Stopped")
  val Finished = Value("Finished")
  val Killed = Value("Killed")
  val NotStarted = Value("NotStarted")
  val Uploaded = Value("Uploaded")
  val NotDefined = Value("NotDefined")
}

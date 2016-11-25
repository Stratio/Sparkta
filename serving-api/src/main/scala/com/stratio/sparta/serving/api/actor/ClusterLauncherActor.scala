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

package com.stratio.sparta.serving.api.actor

import akka.actor.{Actor, ActorRef}
import akka.event.slf4j.SLF4JLogging
import akka.pattern.ask
import akka.util.Timeout
import com.google.common.io.BaseEncoding
import com.stratio.sparta.driver.utils.ClusterSparkFilesUtils
import com.stratio.sparta.serving.api.actor.SparkStreamingContextActor._
import com.stratio.sparta.serving.core.SpartaSerializer
import com.stratio.sparta.serving.core.config.SpartaConfig
import com.stratio.sparta.serving.core.constants.AppConstant
import com.stratio.sparta.serving.core.models.{AggregationPoliciesModel, PolicyStatusModel}
import com.stratio.sparta.serving.core.policy.status.PolicyStatusActor.Update
import com.stratio.sparta.serving.core.policy.status.PolicyStatusEnum
import com.stratio.sparta.serving.core.utils.HdfsUtils
import com.typesafe.config.{Config, ConfigRenderOptions}
import org.apache.spark.launcher.SpartaLauncher

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Properties, Success, Try}

class ClusterLauncherActor(policyStatusActor: ActorRef) extends Actor
  with SLF4JLogging
  with SpartaSerializer {

  private val SpartaDriver = "com.stratio.sparta.driver.SpartaClusterJob"
  private val StandaloneSupervise = "--supervise"

  private val ClusterConfig = SpartaConfig.getClusterConfig.get
  private val ZookeeperConfig = SpartaConfig.getZookeeperConfig.get
  private val HdfsConfig = SpartaConfig.getHdfsConfig.get
  private val DetailConfig = SpartaConfig.getDetailConfig.get
  private val Hdfs = HdfsUtils()

  implicit val timeout: Timeout = Timeout(3.seconds)

  override def receive: PartialFunction[Any, Unit] = {
    case Start(policy: AggregationPoliciesModel) => doInitSpartaContext(policy)
  }

  def doInitSpartaContext(policy: AggregationPoliciesModel): Unit = {
    Try {
      val Uploader = ClusterSparkFilesUtils(policy, Hdfs)
      val PolicyId = policy.id.get.trim
      val Master = ClusterConfig.getString(AppConstant.Master)
      val BasePath = s"/user/${Hdfs.userName}/${AppConstant.ConfigAppName}/$PolicyId"
      val PluginsJarsPath = s"$BasePath/${HdfsConfig.getString(AppConstant.PluginsFolder)}/"
      val DriverJarPath = s"$BasePath/${HdfsConfig.getString(AppConstant.ExecutionJarFolder)}/"

      log.info("Init new cluster streamingContext with name " + policy.name)
      validateSparkHome()
      val driverPath = Uploader.getDriverFile(DriverJarPath)
      val pluginsFiles = Uploader.getPluginsFiles(PluginsJarsPath)
      val driverParams =
        Seq(PolicyId, zkConfigEncoded, detailConfigEncoded, pluginsEncoded(pluginsFiles), hdfsConfigEncoded)

      log.info(s"Launching Sparta Job with options ... \n\tPolicy name: ${policy.name}\n\tMain: $SpartaDriver\n\t" +
        s"Driver path: $driverPath\n\tMaster: $Master\n\tSpark arguments: ${sparkArgs.mkString(",")}\n\t" +
        s"Driver params: $driverParams\n\tPlugins files: ${pluginsFiles.mkString(",")}")
      launch(policy.id.get, policy.name, SpartaDriver, driverPath, Master, sparkArgs, driverParams, pluginsFiles)
    } match {
      case Failure(exception) =>
        log.error(exception.getLocalizedMessage, exception)
        setErrorStatus(policy.id.get)
      case Success(_) =>
        log.info("Sparta Cluster Job launched correctly")
    }
  }

  private def setErrorStatus(policyId: String): Unit =
    policyStatusActor ? Update(PolicyStatusModel(policyId, PolicyStatusEnum.Failed))

  private def sparkHome: String = Properties.envOrElse("SPARK_HOME", ClusterConfig.getString(AppConstant.SparkHome))

  /**
    * Checks if we have a valid Spark home.
    */
  private def validateSparkHome(): Unit = require(Try(sparkHome).isSuccess,
    "You must set the $SPARK_HOME path in configuration or environment")

  /**
    * Checks if supervise param is set when execution mode is standalone or mesos
    *
    * @return The result of checks as boolean value
    */
  def isSupervised: Boolean =
    if (DetailConfig.getString(AppConstant.ExecutionMode) == AppConstant.ConfigStandAlone ||
      DetailConfig.getString(AppConstant.ExecutionMode) == AppConstant.ConfigMesos) {
      Try(ClusterConfig.getBoolean(AppConstant.Supervise)).getOrElse(false)
    } else false

  //scalastyle:off
  private def launch(policyId : String,
                      policyName: String,
                     main: String,
                     hdfsDriverFile: String,
                     master: String,
                     args: Map[String, String],
                     driverParams: Seq[String],
                     pluginsFiles: Seq[String]): Unit = {
    val sparkLauncher = new SpartaLauncher()
      .setSparkHome(sparkHome)
      .setAppResource(hdfsDriverFile)
      .setMainClass(main)
      .setMaster(master)
    // Plugins files
    pluginsFiles.foreach(file => sparkLauncher.addJar(file))

    //Spark arguments
    args.map { case (k: String, v: String) => sparkLauncher.addSparkArg(k, v) }
    if (isSupervised) sparkLauncher.addSparkArg(StandaloneSupervise)

    // Kerberos Options
    val principalNameOption = HdfsUtils.getPrincipalName
    val keyTabPathOption = HdfsUtils.getKeyTabPath
    (principalNameOption, keyTabPathOption) match {
      case (Some(principalName), Some(keyTabPath)) =>
        log.info(s"Launching Spark Submit with Kerberos security, adding principal and keyTab arguments... \n\t" +
          s"Principal: $principalName \n\tKeyTab: $keyTabPath")
        sparkLauncher.addSparkArg("--principal", principalName)
        sparkLauncher.addSparkArg("--keytab", keyTabPath)
      case _ =>
        log.info("Launching Spark Submit without Kerberos security")
    }

    // Spark properties
    log.info("Adding Spark options to Sparta Job ... ")
    sparkConf.foreach { case (key: String, value: String) =>
      val valueParsed = if (key == "spark.app.name") s"$value-$policyName" else value
      log.info(s"\t$key = $valueParsed")
      sparkLauncher.setConf(key, valueParsed)
    }

    // Driver (Sparta) params
    driverParams.foreach(sparkLauncher.addAppArgs(_))

    // Starting Spark Submit Process with Spark Launcher
    log.info("Executing SparkLauncher...")
    val sparkProcessStatus: Future[(Boolean, Process)] = for {
      sparkProcess <- Future(sparkLauncher.asInstanceOf[SpartaLauncher].launch)
    } yield (Await.result(Future(sparkProcess.waitFor() == 0), 20 seconds), sparkProcess)
    sparkProcessStatus.onComplete {
      case Success((exitCode, sparkProcess)) =>
        log.info("Command: {}", sparkLauncher.asInstanceOf[SpartaLauncher].getSubmit)
        if(!sparkLauncherStreams(exitCode, sparkProcess)) throw new Exception("TimeOut in Spark Launcher")
      case Failure(exception) =>
        log.error(exception.getMessage)
        throw exception
    }
  }
  //scalastyle:on

  private def sparkLauncherStreams(exitCode: Boolean, sparkProcess: Process): Boolean = {

    def recursiveErrors(it: Iterator[String], count: Int): Unit = {
      log.info(it.next())
      if (it.hasNext && count < 50)
        recursiveErrors(it, count + 1)
    }

    if (exitCode) log.info("Spark process exited successfully")
    else log.info("Spark process exited with timeout")

    Source.fromInputStream(sparkProcess.getInputStream).close()
    sparkProcess.getOutputStream.close()

    log.info("ErrorStream:")

    val error = Source.fromInputStream(sparkProcess.getErrorStream)
    recursiveErrors(error.getLines(), 0)
    error.close()

    exitCode
  }

  private def sparkArgs: Map[String, String] =
    ClusterLauncherActor.toMap(AppConstant.DeployMode, "--deploy-mode", ClusterConfig) ++
      ClusterLauncherActor.toMap(AppConstant.Name, "--name", ClusterConfig) ++
      ClusterLauncherActor.toMap(AppConstant.PropertiesFile, "--properties-file", ClusterConfig) ++
      ClusterLauncherActor.toMap(AppConstant.TotalExecutorCores, "--total-executor-cores", ClusterConfig) ++
      // Yarn only
      ClusterLauncherActor.toMap(AppConstant.YarnQueue, "--queue", ClusterConfig) ++
      ClusterLauncherActor.toMap(AppConstant.Files, "--files", ClusterConfig) ++
      ClusterLauncherActor.toMap(AppConstant.Archives, "--archives", ClusterConfig) ++
      ClusterLauncherActor.toMap(AppConstant.AddJars, "--addJars", ClusterConfig) ++
      ClusterLauncherActor.toMap(AppConstant.NumExecutors, "--num-executors", ClusterConfig) ++
      ClusterLauncherActor.toMap(AppConstant.DriverCores, "--driver-cores", ClusterConfig) ++
      ClusterLauncherActor.toMap(AppConstant.DriverMemory, "--driver-memory", ClusterConfig) ++
      ClusterLauncherActor.toMap(AppConstant.ExecutorCores, "--executor-cores", ClusterConfig) ++
      ClusterLauncherActor.toMap(AppConstant.ExecutorMemory, "--executor-memory", ClusterConfig)

  private def render(config: Config, key: String): String = config.atKey(key).root.render(ConfigRenderOptions.concise)

  private def encode(value: String): String = BaseEncoding.base64().encode(value.getBytes)

  private def zkConfigEncoded: String = encode(render(ZookeeperConfig, "zookeeper"))

  private def detailConfigEncoded: String = encode(render(DetailConfig, "config"))

  private def pluginsEncoded(plugins: Seq[String]): String = encode((Seq(" ") ++ plugins).mkString(","))

  private def hdfsConfigEncoded: String = encode(render(HdfsConfig, "hdfs"))

  private def sparkConf: Seq[(String, String)] =
    ClusterConfig.entrySet()
      .filter(_.getKey.startsWith("spark.")).toSeq
      .map(e => (e.getKey, e.getValue.unwrapped.toString))
}

object ClusterLauncherActor extends SLF4JLogging {

  def toMap(key: String, newKey: String, config: Config): Map[String, String] =
    Try(config.getString(key)) match {
      case Success(value) =>
        Map(newKey -> value)
      case Failure(_) =>
        log.debug(s"The key $key was not defined in config.")
        Map.empty[String, String]
    }
}
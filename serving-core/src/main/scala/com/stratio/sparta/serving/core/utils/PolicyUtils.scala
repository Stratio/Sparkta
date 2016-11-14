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

import java.io.File
import java.util.UUID

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.serving.core.config.SpartaConfig
import com.stratio.sparta.serving.core.constants.{ActorsConstant, AppConstant}
import com.stratio.sparta.serving.core.models.{AggregationPoliciesModel, SpartaSerializer}
import com.stratio.sparta.serving.core.curator.CuratorFactoryHolder
import org.apache.commons.io.FileUtils
import org.apache.curator.framework.CuratorFramework
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization._

import scala.collection.JavaConversions
import scala.util._

trait PolicyUtils extends SpartaSerializer with SLF4JLogging {

  def existsByName(name: String, id: Option[String] = None, curatorFramework: CuratorFramework): Boolean = {
    val nameToCompare = name.toLowerCase
    Try {
      if (existsPath)
        getPolicies(curatorFramework).exists(byName(id, nameToCompare))
      else {
        log.warn(s"Zookeeper path for policies doesn't exists. It will be created.")
        false
      }
    } match {
      case Success(result) => result
      case Failure(exception) =>
        log.error(exception.getLocalizedMessage, exception)
        false
    }
  }

  def existsPath: Boolean = CuratorFactoryHolder.existsPath(AppConstant.PoliciesBasePath)

  def byName(id: Option[String], nameToCompare: String): (AggregationPoliciesModel) => Boolean = {
    policy =>
      if (id.isDefined)
        policy.name == nameToCompare && policy.id.get != id.get
      else policy.name == nameToCompare
  }

  def savePolicyInZk(policy: AggregationPoliciesModel, curatorFramework: CuratorFramework): Unit = {

    Try {
      populatePolicy(policy, curatorFramework)
    } match {
      case Success(_) => log.info(s"Policy ${policy.id.get} already in zookeeper. Updating it...")
        updatePolicy(policy, curatorFramework)
      case Failure(e) => writePolicy(policy, curatorFramework)
    }
  }

  def writePolicy(policy: AggregationPoliciesModel, curatorFramework: CuratorFramework): Unit = {
    curatorFramework.create().creatingParentsIfNeeded().forPath(
      s"${AppConstant.PoliciesBasePath}/${policy.id.get}", write(policy).getBytes)
  }

  def updatePolicy(policy: AggregationPoliciesModel, curatorFramework: CuratorFramework): Unit = {
    curatorFramework.setData().forPath(s"${AppConstant.PoliciesBasePath}/${policy.id.get}", write(policy).getBytes)
  }

  def populatePolicy(policy: AggregationPoliciesModel, curatorFramework: CuratorFramework): AggregationPoliciesModel = {
    read[AggregationPoliciesModel](new Predef.String(curatorFramework.getData.forPath(
      s"${AppConstant.PoliciesBasePath}/${policy.id.get}")))
  }

  def policyWithId(policy: AggregationPoliciesModel): AggregationPoliciesModel =
    (policy.id match {
      case None => populatePolicyWithRandomUUID(policy)
      case Some(_) => policy
    }).copy(name = policy.name.toLowerCase, version = Some(ActorsConstant.UnitVersion))

  def populatePolicyWithRandomUUID(policy: AggregationPoliciesModel): AggregationPoliciesModel = {
    policy.copy(id = Some(UUID.randomUUID.toString))
  }

  def isLocalMode: Boolean =
    SpartaConfig.getDetailConfig match {
      case Some(detailConfig) => detailConfig.getString(AppConstant.ExecutionMode).equalsIgnoreCase("local")
      case None => true
    }

  def existsByNameId(name: String, id: Option[String] = None, curatorFramework: CuratorFramework):
  Option[AggregationPoliciesModel] = {
    val nameToCompare = name.toLowerCase
    Try {
      if (existsPath) {
        getPolicies(curatorFramework)
          .find(policy => if (id.isDefined) policy.id.get == id.get else policy.name == nameToCompare)
      } else None
    } match {
      case Success(result) => result
      case Failure(exception) =>
        log.error(exception.getLocalizedMessage, exception)
        None
    }
  }

  def setVersion(lastPolicy: AggregationPoliciesModel, newPolicy: AggregationPoliciesModel): Option[Int] = {
    if (lastPolicy.cubes != newPolicy.cubes) {
      lastPolicy.version match {
        case Some(version) => Some(version + ActorsConstant.UnitVersion)
        case None => Some(ActorsConstant.UnitVersion)
      }
    } else lastPolicy.version
  }

  def getPolicies(curatorFramework: CuratorFramework): List[AggregationPoliciesModel] = {
    val children = curatorFramework.getChildren.forPath(AppConstant.PoliciesBasePath)
    JavaConversions.asScalaBuffer(children).toList.map(element =>
      read[AggregationPoliciesModel](new Predef.String(curatorFramework.getData.
        forPath(s"${AppConstant.PoliciesBasePath}/$element"))))
  }

  def byId(id: String, curatorFramework: CuratorFramework): AggregationPoliciesModel =
    read[AggregationPoliciesModel](
      new Predef.String(curatorFramework.getData.forPath(s"${AppConstant.PoliciesBasePath}/$id")))

  def deleteRelatedPolicies(policies: Seq[AggregationPoliciesModel]): Unit = {
    policies.foreach(deleteCheckpointPath)
  }

  /**
   * Method to parse AggregationPoliciesModel from JSON string
   *
   * @param json The policy as JSON string
   * @return AggregationPoliciesModel
   */
  def parseJson(json: String): AggregationPoliciesModel = parse(json).extract[AggregationPoliciesModel]

  def jarsFromPolicy(apConfig: AggregationPoliciesModel): Seq[File] = {
    apConfig.userPluginsJars.filter(!_.jarPath.isEmpty).map(_.jarPath).distinct.map(filePath => new File(filePath))
  }


  /** CHECKPOINT OPTIONS **/

  def deleteFromLocal(policy: AggregationPoliciesModel): Unit =
    FileUtils.deleteDirectory(new File(checkpointPath(policy)))

  def deleteFromHDFS(policy: AggregationPoliciesModel): Unit =
    HdfsUtils(SpartaConfig.getHdfsConfig).delete(checkpointPath(policy))

  def checkpointGoesToHDFS: Boolean =
    Option(System.getenv("HADOOP_CONF_DIR")) match {
      case Some(_) => true
      case None => false
    }

  private def cleanCheckpointPath(path: String) : String = {
    val hdfsPrefix = "hdfs://"

    if(path.startsWith(hdfsPrefix))
      log.info(s"The path starts with $hdfsPrefix and is not valid, it is replaced with empty value")
    path.replace(hdfsPrefix, "")
  }

  private def checkpointPathFromProperties(policyName: String): String =
    (for {
      config <- SpartaConfig.getDetailConfig
      checkpointPath <- Try(cleanCheckpointPath(config.getString(AppConstant.ConfigCheckpointPath))).toOption
    } yield s"$checkpointPath/$policyName").getOrElse(generateDefaultCheckpointPath)

  private def autoDeleteCheckpointPathFromProperties: Boolean =
    Try(SpartaConfig.getDetailConfig.get.getBoolean(AppConstant.ConfigAutoDeleteCheckpoint))
      .getOrElse(AppConstant.DefaultAutoDeleteCheckpoint)

  private def generateDefaultCheckpointPath: String =
    SpartaConfig.getDetailConfig.map(_.getString(AppConstant.ExecutionMode)) match {
      case Some(mode) if mode == AppConstant.ConfigMesos || mode == AppConstant.ConfigYarn =>
        AppConstant.DefaultCheckpointPathClusterMode +
          Try(SpartaConfig.getHdfsConfig.get.getString(AppConstant.HadoopUserName))
            .getOrElse(AppConstant.DefaultHdfsUser) +
          AppConstant.DefaultHdfsUser
      case Some(AppConstant.ConfigLocal) =>
        AppConstant.DefaultCheckpointPathLocalMode
      case _ =>
        throw new RuntimeException("Error getting execution mode")
    }

  def deleteCheckpointPath(policy: AggregationPoliciesModel): Unit = {
    Try {
      if (!isLocalMode || checkpointGoesToHDFS)
        deleteFromHDFS(policy)
      else deleteFromLocal(policy)
    } match {
      case Success(_) => log.info(s"Checkpoint deleted in folder: ${checkpointPath(policy)}")
      case Failure(ex) => log.error("Cannot delete checkpoint folder", ex)
    }
  }

  def checkpointPath(policy: AggregationPoliciesModel): String =
    policy.checkpointPath.map { path =>
      s"${cleanCheckpointPath(path)}/${policy.name}"
    } getOrElse checkpointPathFromProperties(policy.name)

  def autoDeleteCheckpointPath(policy: AggregationPoliciesModel): Boolean =
    policy.autoDeleteCheckpoint.getOrElse(autoDeleteCheckpointPathFromProperties)
}

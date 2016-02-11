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

package com.stratio.sparkta.serving.api.actor

import java.io.File
import java.util.UUID
import com.stratio.sparkta.serving.core.dao.ConfigDAO

import scala.collection.JavaConversions
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import akka.actor.Actor
import akka.actor.ActorRef
import akka.event.slf4j.SLF4JLogging
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.KeeperException.NoNodeException
import org.json4s.jackson.Serialization.read
import org.json4s.jackson.Serialization.write
import com.stratio.sparkta.driver.util.HdfsUtils
import com.stratio.sparkta.serving.api.actor.PolicyActor._
import com.stratio.sparkta.serving.api.constants.ActorsConstant
import com.stratio.sparkta.serving.core.constants.AppConstant
import com.stratio.sparkta.serving.core.exception.ServingCoreException
import com.stratio.sparkta.serving.core.models._
import com.stratio.sparkta.serving.core.policy.status.PolicyStatusActor
import com.stratio.sparkta.serving.core.policy.status.PolicyStatusEnum
import com.stratio.sparkta.serving.core.CuratorFactoryHolder
import com.stratio.sparkta.serving.core.SparktaConfig

/**
 * Implementation of supported CRUD operations over ZK needed to manage policies.
 */
class PolicyActor(curatorFramework: CuratorFramework, policyStatusActor: ActorRef)
  extends Actor
    with SLF4JLogging
    with SparktaSerializer {

  override def receive: Receive = {
    case Create(policy) => create(policy)
    case Update(policy) => update(policy)
    case Delete(id) => delete(id)
    case Find(id) => find(id)
    case FindByName(name) => findByName(name.toLowerCase)
    case FindAll() => findAll()
    case FindByFragment(fragmentType, id) => findByFragment(fragmentType, id)
  }

  def findAll(): Unit =
    sender ! ResponsePolicies(Try({
      val children = curatorFramework.getChildren.forPath(s"${AppConstant.PoliciesBasePath}")
      JavaConversions.asScalaBuffer(children).toList.map(element =>
        byId(element)).toSeq
    }).recover {
      case e: NoNodeException => Seq()
    })

  def findByFragment(fragmentType: String, id: String): Unit =
    sender ! ResponsePolicies(Try({
      val children = curatorFramework.getChildren.forPath(s"${AppConstant.PoliciesBasePath}")
      JavaConversions.asScalaBuffer(children).toList.map(element =>
        byId(element)).filter(apm =>
        apm.fragments.exists(f => f.id.get == id)).toSeq
    }).recover {
      case e: NoNodeException => Seq()
    })

  def find(id: String): Unit =
    sender ! new ResponsePolicy(Try({
      byId(id)
    }).recover {
      case e: NoNodeException => throw new ServingCoreException(ErrorModel.toString(
        new ErrorModel(ErrorModel.CodeNotExistsPolicytWithId, s"No policy with id $id.")
      ))
    })

  private def byId(id: String): CommonPoliciesModel = read[CommonPoliciesModel](
    new Predef.String(curatorFramework.getData.forPath(s"${AppConstant.PoliciesBasePath}/$id")))

  def findByName(name: String): Unit =
    sender ! ResponsePolicy(Try({
      val children = curatorFramework.getChildren.forPath(s"${AppConstant.PoliciesBasePath}")
      JavaConversions.asScalaBuffer(children).toList.map(element =>
        byId(element)).filter(policy => policy.name == name).head
    }).recover {
      case e: NoNodeException => throw new ServingCoreException(ErrorModel.toString(
        new ErrorModel(ErrorModel.CodeNotExistsPolicytWithName, s"No policy with name $name.")
      ))
      case e: NoSuchElementException => throw new ServingCoreException(ErrorModel.toString(
        new ErrorModel(ErrorModel.CodeNotExistsPolicytWithName, s"No policy with name $name.")
      ))
    })

  def associateStatus(model: CommonPoliciesModel): Unit = {
    policyStatusActor ! PolicyStatusActor.Create(PolicyStatusModel(model.id.get, PolicyStatusEnum.NotStarted))
  }

  def create(policy: CommonPoliciesModel): Unit =
    sender ! ResponsePolicy(Try({
      val searchPolicy = existsByNameId(policy.name)
      if (searchPolicy.isDefined) {
        throw new ServingCoreException(ErrorModel.toString(
          new ErrorModel(ErrorModel.CodeExistsPolicytWithName,
            s"Policy with name ${policy.name} exists. The actual policty id is: ${searchPolicy.get.id}")
        ))
      }
      val policyS = policy.copy(id = Some(s"${UUID.randomUUID.toString}"),
        name = policy.name.toLowerCase,
        version = Some(ActorsConstant.UnitVersion))
      curatorFramework.create().creatingParentsIfNeeded().forPath(
        s"${AppConstant.PoliciesBasePath}/${policyS.id.get}", write(policyS).getBytes)

      associateStatus(policyS)

      policyS
    }))

  def update(policy: CommonPoliciesModel): Unit = {
    sender ! Response(Try({
      val searchPolicy = existsByNameId(policy.name, policy.id)
      if (searchPolicy.isEmpty) {
        throw new ServingCoreException(ErrorModel.toString(
          new ErrorModel(ErrorModel.CodeExistsPolicytWithName,
            s"Policy with name ${policy.name} not exists.")
        ))
      } else {
        val policyS = policy.copy(
          name = policy.name.toLowerCase,
          version = setVersion(searchPolicy.get, policy))
        deleteCheckpointPath(policy)
        curatorFramework.setData.forPath(s"${AppConstant.PoliciesBasePath}/${policyS.id.get}", write(policyS).getBytes)
      }
    }).recover {
      case e: NoNodeException => throw new ServingCoreException(ErrorModel.toString(
        new ErrorModel(ErrorModel.CodeNotExistsPolicytWithId, s"No policy with id ${policy.id.get}.")
      ))
    })
  }

  def delete(id: String): Unit =
    sender ! Response(Try({
      curatorFramework.delete().forPath(s"${AppConstant.PoliciesBasePath}/$id")
    }).recover {
      case e: NoNodeException => throw new ServingCoreException(ErrorModel.toString(
        new ErrorModel(ErrorModel.CodeNotExistsFragmentWithId,
          s"No policy with id $id.")
      ))
    })

  def existsByNameId(name: String, id: Option[String] = None): Option[CommonPoliciesModel] = {
    val nameToCompare = name.toLowerCase
    Try({
      val basePath = s"${AppConstant.PoliciesBasePath}"
      if (CuratorFactoryHolder.existsPath(basePath)) {
        val children = curatorFramework.getChildren.forPath(basePath)
        JavaConversions.asScalaBuffer(children).toList.map(element =>
          read[CommonPoliciesModel](new String(curatorFramework.getData.forPath(s"$basePath/$element"))))
          .find(policy => if (id.isDefined) policy.id.get == id.get else policy.name == nameToCompare)
      } else None
    }) match {
      case Success(result) => result
      case Failure(exception) => {
        log.error(exception.getLocalizedMessage, exception)
        None
      }
    }
  }

  def setVersion(lastPolicy: CommonPoliciesModel, newPolicy: CommonPoliciesModel): Option[Int] = {
    if (lastPolicy.cubes != newPolicy.cubes) {
      lastPolicy.version match {
        case Some(version) => Some(version + ActorsConstant.UnitVersion)
        case None => Some(ActorsConstant.UnitVersion)
      }
    } else lastPolicy.version
  }
}

object PolicyActor {

  case class Create(policy: CommonPoliciesModel)

  case class Update(policy: CommonPoliciesModel)

  case class Delete(name: String)

  case class FindAll()

  case class Find(id: String)

  case class FindByName(name: String)

  case class FindByFragment(fragmentType: String, id: String)

  case class Response(status: Try[_])

  case class ResponsePolicies(policies: Try[Seq[CommonPoliciesModel]])

  case class ResponsePolicy(policy: Try[CommonPoliciesModel])

  def deleteCheckpointPath(policy: CommonPoliciesModel): Unit = {
    if (SparktaConfig.getClusterConfig.isDefined) {
      val configDAO = ConfigDAO(SparktaConfig.mainConfig.get)
      val hdfsJsonConfig = configDAO.dao.get(AppConstant.HdfsID).get
      val config = ConfigFactory.parseString(hdfsJsonConfig).getConfig(AppConstant.HdfsID)
      HdfsUtils(config).delete(CommonPoliciesModel.checkpointPath(policy))
    } else {
      FileUtils.deleteDirectory(new File(CommonPoliciesModel.checkpointPath(policy)))
    }
  }
}
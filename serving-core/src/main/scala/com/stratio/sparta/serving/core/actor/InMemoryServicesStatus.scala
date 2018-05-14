/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.serving.core.actor

import akka.event.slf4j.SLF4JLogging
import akka.persistence._
import akka.serialization.SerializationExtension
import com.stratio.sparta.serving.core.actor.DebugWorkflowPublisherActor.{DebugWorkflowChange, DebugWorkflowRemove}
import com.stratio.sparta.serving.core.actor.ExecutionPublisherActor.{ExecutionChange, ExecutionRemove}
import com.stratio.sparta.serving.core.actor.GroupPublisherActor.{GroupChange, GroupRemove}
import com.stratio.sparta.serving.core.actor.StatusPublisherActor.{StatusChange, StatusRemove}
import com.stratio.sparta.serving.core.actor.WorkflowPublisherActor._
import com.stratio.sparta.serving.core.models.workflow._

trait InMemoryServicesStatus extends PersistentActor with SLF4JLogging {

  var workflowsWithEnv = scala.collection.mutable.Map[String, Workflow]()
  var workflowsRaw = scala.collection.mutable.Map[String, Workflow]()
  var statuses = scala.collection.mutable.Map[String, WorkflowStatus]()
  var executions = scala.collection.mutable.Map[String, WorkflowExecution]()
  var groups = scala.collection.mutable.Map[String, Group]()
  var debugWorkflows = scala.collection.mutable.Map[String, DebugWorkflow]()
  val snapShotInterval = 1000
  val serialization = SerializationExtension(context.system)
  val serializer = serialization.findSerializerFor(SnapshotState(
    workflowsWithEnv,
    workflowsRaw,
    statuses,
    executions,
    groups,
    debugWorkflows
  ))

  def addWorkflowsWithEnv(workflow: Workflow): Unit =
    workflow.id.foreach(id => workflowsWithEnv += (id -> workflow))

  def addDebugWorkflow(debugWorkflow: DebugWorkflow): Unit =
    debugWorkflow.workflowOriginal.id.foreach(id => debugWorkflows += (id -> debugWorkflow))

  def addStatus(status: WorkflowStatus): Unit =
    statuses += (status.id -> status)

  def addExecution(execution: WorkflowExecution): Unit =
    executions += (execution.id -> execution)

  def removeStatus(id: String): Unit =
    statuses -= id

  def removeExecution(id: String): Unit =
    executions -= id

  def addGroup(group: Group): Unit =
    group.id.foreach(id => groups += (id -> group))

  def removeGroup(group: Group): Unit =
    group.id.foreach(id => groups -= id)

  def addWorkflowsRaw(workflow: Workflow): Unit =
    workflow.id.foreach(id => workflowsRaw += (id -> workflow))

  def removeWorkflowsRaw(workflow: Workflow): Unit =
    workflow.id.foreach(id => workflowsRaw -= id)

  def removeWorkflowsWithEnv(workflow: Workflow): Unit =
    workflow.id.foreach(id => workflowsWithEnv -= id)

  def removeDebugWorkflow(debugWorkflow: DebugWorkflow): Unit =
    debugWorkflow.workflowOriginal.id.foreach(id => debugWorkflows -= id)

  val receiveRecover: Receive = eventsReceive.orElse(snapshotRecover).orElse(recoverComplete)

  //scalastyle:off
  def eventsRecover: Receive = {
    case StatusChange(_, status) => addStatus(status)
    case StatusRemove(_, status) => removeStatus(status.id)
    case WorkflowChange(_, workflow) => addWorkflowsWithEnv(workflow)
    case GroupChange(_, group) => addGroup(group)
    case WorkflowRawChange(_, workflow) => addWorkflowsRaw(workflow)
    case ExecutionChange(_, execution) => addExecution(execution)
    case ExecutionRemove(_, execution) => removeExecution(execution.id)
    case WorkflowRemove(_, workflow) => removeWorkflowsWithEnv(workflow)
    case WorkflowRawRemove(_, workflow) => removeWorkflowsRaw(workflow)
    case GroupRemove(_, group) => removeGroup(group)
    case DebugWorkflowChange(_, debugWorkflow) => addDebugWorkflow(debugWorkflow)
    case DebugWorkflowRemove(_, debugWorkflow) => removeDebugWorkflow(debugWorkflow)
  }

  def snapshotRecover: Receive = {
    case SnapshotOffer(_, snapshotJson: Array[Byte]) =>
      val snapshot = serializer.fromBinary(snapshotJson).asInstanceOf[SnapshotState]

      workflowsWithEnv = snapshot.workflowsWithEnv
      workflowsRaw = snapshot.workflowsRaw
      statuses = snapshot.statuses
      executions = snapshot.executions
      groups = snapshot.groups
      debugWorkflows = snapshot.debugWorkflows
  }

  def recoverComplete: Receive = {
    case RecoveryCompleted =>
      log.info(s"Recovery complete for Actor id: $persistenceId")
  }

  def snapshotSaveNotificationReceive: Receive = {
    case SaveSnapshotFailure(metadata, reason) =>
      log.error(s"Snapshot failed to save: Metadata -> $metadata. Reason -> ${reason.toString}")

    case SaveSnapshotSuccess(metadata) =>
      log.info(s"Snapshot saved successfully: Metadata -> $metadata")
  }

  def checkSaveSnapshot(): Unit = {
    if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0) {
      val bytes = serializer.toBinary(SnapshotState(
        workflowsWithEnv,
        workflowsRaw,
        statuses,
        executions,
        groups,
        debugWorkflows
      ))
      saveSnapshot(bytes)
    }
  }

  def eventsReceive: Receive = {
    case request@StatusChange(path, status) =>
      persist(request) { case stChange =>
        addStatus(stChange.workflowStatus)
        checkSaveSnapshot()
      }
    case request@StatusRemove(path, status) =>
      persist(request) { case stRemove =>
        removeStatus(stRemove.workflowStatus.id)
        checkSaveSnapshot()
      }
    case request@WorkflowChange(path, workflow) =>
      persist(request) { case wChange =>
        addWorkflowsWithEnv(wChange.workflow)
        checkSaveSnapshot()
      }
    case request@GroupChange(path, group) =>
      persist(request) { case gChange =>
        addGroup(gChange.group)
        checkSaveSnapshot()
      }
    case request@WorkflowRawChange(path, workflow) =>
      persist(request) { case wChange =>
        addWorkflowsRaw(wChange.workflow)
        checkSaveSnapshot()
      }
    case request@ExecutionChange(path, execution) =>
      persist(request) { case eChange =>
        addExecution(eChange.execution)
        checkSaveSnapshot()
      }
    case request@ExecutionRemove(path, execution) =>
      persist(request) { case eRemove =>
        removeExecution(eRemove.execution.id)
        checkSaveSnapshot()
      }
    case request@WorkflowRemove(path, workflow) =>
      persist(request) { case wRemove =>
        removeWorkflowsWithEnv(wRemove.workflow)
        checkSaveSnapshot()
      }
    case request@WorkflowRawRemove(path, workflow) =>
      persist(request) { case wRemove =>
        removeWorkflowsRaw(wRemove.workflow)
        checkSaveSnapshot()
      }
    case request@GroupRemove(path, group) =>
      persist(request) { case gRemove =>
        removeGroup(gRemove.group)
        checkSaveSnapshot()
      }
    case request@DebugWorkflowChange(path, debugWorkflow) =>
      persist(request) { case wChange =>
        addDebugWorkflow(wChange.debugWorkflow)
        checkSaveSnapshot()
      }
    case request@DebugWorkflowRemove(path, debugWorkflow) =>
      persist(request) { case wRemove =>
        removeDebugWorkflow(wRemove.debugWorkflow)
        checkSaveSnapshot()
      }
  }
}

case class SnapshotState(
                          workflowsWithEnv: scala.collection.mutable.Map[String, Workflow],
                          workflowsRaw: scala.collection.mutable.Map[String, Workflow],
                          statuses: scala.collection.mutable.Map[String, WorkflowStatus],
                          executions: scala.collection.mutable.Map[String, WorkflowExecution],
                          groups: scala.collection.mutable.Map[String, Group],
                          debugWorkflows: scala.collection.mutable.Map[String, DebugWorkflow]
                        )

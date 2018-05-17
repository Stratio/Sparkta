/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

package com.stratio.sparta.serving.core.dao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

import com.stratio.sparta.serving.core.utils.{JdbcSlickUtils, PostgresJdbcSlickImpl}

//scalastyle:off
trait ExecutionHistoryDao {

  this: JdbcSlickUtils =>

  import profile.api._

  import com.stratio.sparta.serving.core.actor.ExecutionHistoryActor._

  val table = workflowExecutionHistoryTable

  def insert(workflowExecution: WorkflowExecutionHistory): Future[_] = {
    val dbioAction = DBIO.seq(
      table += workflowExecution
    ).transactionally
    db.run(txHandler(dbioAction))
  }

  private def txHandler(dbioAction: DBIOAction[Unit, NoStream, Effect.Write with Effect.Transactional]) = {
    val txHandler = dbioAction.asTry.flatMap {
      case Failure(e: Throwable) => DBIO.failed(e)
      case Success(s) => DBIO.successful(s)
    }
    txHandler
  }

  def update(workflowExecution: WorkflowExecutionHistory): Future[_] = {
    val updateQuery = for {
      data <- table
      if data.id === workflowExecution.id
    } yield data
    val dbioAction = DBIO.seq(
      updateQuery.update(workflowExecution)
    ).transactionally
    db.run(txHandler(dbioAction))
  }

  def createSchema(): Future[Unit] = {
    val dbioAction = (for {
      _ <- table.schema.create
    } yield ()).transactionally
    db.run(dbioAction)
  }

  def selectAll(): Future[List[WorkflowExecutionHistory]] = {
    db.run(table.result).map(_.toList)
  }

  def findByWorkflowId(workflowId: String): Future[List[WorkflowExecutionHistory]] = {
    db.run(table.filter(_.id === workflowId).result).map(_.toList)
  }

  def findByUserId(userId: String): Future[List[WorkflowExecutionHistory]] = {
    db.run(table.filter(_.userId === userId).result).map(_.toList)
  }
}

trait ExecutionHistoryDaoImpl extends ExecutionHistoryDao with PostgresJdbcSlickImpl
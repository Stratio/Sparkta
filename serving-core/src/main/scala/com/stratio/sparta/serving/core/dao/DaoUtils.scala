/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

package com.stratio.sparta.serving.core.dao

import javax.cache.Cache
import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

import akka.event.slf4j.SLF4JLogging
import org.apache.ignite.IgniteCache
import org.apache.ignite.cache.query.ScanQuery
import org.apache.ignite.lang.{IgniteFuture, IgniteInClosure}
import slick.ast.BaseTypedType
import slick.relational.RelationalProfile

import com.stratio.sparta.core.properties.ValidatingPropertyMap._
import com.stratio.sparta.serving.core.config.SpartaConfig
import com.stratio.sparta.serving.core.constants.AppConstant
import com.stratio.sparta.serving.core.models.SpartaSerializer
import com.stratio.sparta.serving.core.utils.{JdbcSlickUtils, PostgresDaoFactory}

//scalastyle:off
trait DaoUtils extends JdbcSlickUtils with SLF4JLogging with SpartaSerializer {

  implicit val exc = PostgresDaoFactory.pgExecutionContext

  import profile.api._

  type Id // The type of the PK
  type SpartaEntity // The type of the record inside the table
  type SpartaTable <: RelationalProfile#Table[SpartaEntity] // The table TYPE whose schema was defined through a case class

  def $id(table: SpartaTable): Rep[Id] // Method that returns the PrimaryKey from table

  val table: TableQuery[SpartaTable] // The table itself
  private lazy val tableName = table.baseTableRow.tableName

  // Check if the property is defined in the schemaName configuration (sparta.postgres.schemaName),
  // if not, check if currentSchema is defined in the extraParameters otherwise set to public
  val dbSchemaName: Option[String] = {

    lazy val schemaNameFromConfig: Option[String] =
      Try(SpartaConfig.getPostgresConfig().get.getString("schemaName")).toOption.notBlank

    lazy val schemaNameFromExtraParams: Option[String] =
      Try(SpartaConfig.getPostgresConfig().get.getString("extraParams")).toOption.notBlank
        .fold(Option("")) { extraParameters =>
          extraParameters.split("&", -1)
            .find(condition => condition.startsWith("currentSchema"))
            .map(_.replace("currentSchema=", ""))
        }.notBlank

    schemaNameFromConfig.orElse(schemaNameFromExtraParams).orElse(Some("public"))
  }

  // The method finds automagically a BaseTypedType[Id]  in the context...
  def baseTypedType: BaseTypedType[Id]

  // whereas the val provides the implicit transformation for mapping it in the def filterById
  private[dao] implicit lazy val btt: BaseTypedType[Id] = baseTypedType

  def filterById(id: Id): Query[SpartaTable, SpartaEntity, Seq] = {
    table.filter($id(_) === id)
  }

  // Schema creation

  def createSchema(): Unit = {
    dbSchemaName.foreach { schemaName =>
      val createSchemaSql = s"create schema if not exists $schemaName;"
      db.run(sqlu"#$createSchemaSql").onComplete {
        case Success(_) =>
          log.debug(s"Schema $schemaName created if not exists")
        case Failure(e) =>
          throw e
      }
    }
  }

  // Table creation

  def tableCreation(name: String) = {
    db.run(table.exists.result) onComplete {
      case Success(result) =>
        if (result)
          log.info(s"Table $name already exists: skipping creation")
        else doCreateTable(name)
      case Failure(_) =>
        doCreateTable(name)
    }
  }

  def doCreateTable(name: String) = {
    log.info(s"Creating table $name")
    val dbioAction = (for {
      _ <- table.schema.create
    } yield ()).transactionally
    db.run(txHandler(dbioAction))
  }

  def createTableSchema(): Unit = {
    tableCreation(tableName)
  }

  // Table data initialization

  def initializeData(): Unit = {}

  // CRUD Operations

  def count: Future[Int] =
    db.run(table.size.result)

  def upsert(item: SpartaEntity): Future[_] = {
    val dbioAction = DBIO.seq(upsertAction(item)).transactionally

    db.run(txHandler(dbioAction))
  }

  def upsertAction(item: SpartaEntity) = table.insertOrUpdate(item)

  def findAll(): Future[List[SpartaEntity]] =
    db.run(table.sortBy($id(_).desc).result).map(_.toList)

  def findByID(id: Id): Future[Option[SpartaEntity]] =
    db.run(filterById(id).result.headOption)

  def create(item: SpartaEntity): Future[_] =
    db.run(table += item)

  def createAndReturn(item: SpartaEntity): Future[SpartaEntity] =
    db.run(table returning table += item)

  def createAndReturnList(items: Seq[SpartaEntity]): Future[_] = {
    val dbioAction = (table returning table ++= items).transactionally

    db.run(dbioAction)
  }

  def deleteByID(id: Id): Future[_] = {
    val dbioAction = DBIO.seq(deleteByIDAction(id)).transactionally
    db.run(txHandler(dbioAction))
  }

  def deleteByIDAction(id: Id) = filterById(id).delete

  def createList(items: Seq[SpartaEntity]): Future[_] = {
    val dbioAction = DBIO.seq(table ++= items).transactionally

    db.run(dbioAction)
  }

  def upsertList(items: Seq[SpartaEntity]): Future[_] = {
    val itemsToUpsert = items.map(item => table.insertOrUpdate(item))
    val dbioAction = DBIO.sequence(itemsToUpsert).transactionally

    db.run(dbioAction)
  }

  def deleteList(items: Seq[Id]): Future[_] = {
    val dbioAction = DBIO.seq(table.filter($id(_) inSet items).delete).transactionally

    db.run(dbioAction)
  }

  def deleteAll(): Future[Unit] =
    db.run(txHandler(DBIO.seq(table.delete).transactionally))

  def executeTxDBIO(dbioActions: DBIOAction[Unit, NoStream, Effect.All with Effect.Transactional]*): Future[Unit] = {
    db.run(txHandler(DBIO.seq(dbioActions: _*).transactionally))
  }

  def txHandler(dbioAction: DBIOAction[Unit, NoStream, Effect.All with Effect.Transactional]) = {
    dbioAction.asTry.flatMap {
      case Success(s) => DBIO.successful(s)
      case Failure(e) => DBIO.failed(e)
    }
  }

  def txHandler(dbioAction: Seq[DBIOAction[Unit, NoStream, Effect.All with Effect.Transactional]]) = {
    dbioAction.map(action => action.asTry.flatMap {
      case Success(s) => DBIO.successful(s)
      case Failure(e) => DBIO.failed(e)
    })
  }

  /** Cache implicits methods */

  val cacheEnabled = Try(SpartaConfig.getIgniteConfig().get.getBoolean(AppConstant.IgniteEnabled)).getOrElse(false)

  implicit val cache: IgniteCache[String, SpartaEntity]

  def initialCacheLoad() =
    if (cacheEnabled)
      findAll().map(list =>
        cache.putAll(list.map(element => (getSpartaEntityId(element), element)).toMap[String, SpartaEntity].asJava)
      )

  def cacheById(id: String)(f: => Future[SpartaEntity]): Future[SpartaEntity] = cache.getAsync(id).toScalaFuture.recoverWith { case _ => f }

  //Method to be override in every daos
  def getSpartaEntityId(entity: SpartaEntity): String = "N/A"

  //Generic predicate execution over ignite cache, based in scanQuery defined in every method that performs queries over cache values
  def predicateHead(s: ScanQuery[String, SpartaEntity])(ps: => Future[SpartaEntity]) = {
    Future(cache.query(s).iterator().asScala.map(_.getValue).toList.headOption.getOrElse(throw new RuntimeException("Not found in cache")))
      .recoverWith { case _ => ps }
  }

  def predicateList(s: ScanQuery[String, SpartaEntity])(ps: => Future[Seq[SpartaEntity]]) = {
    Future(cache.query(s).iterator().asScala.map(_.getValue).toList)
      .recoverWith { case _ => ps }
  }

  implicit class CacheIterator[K, V](iterator: java.util.Iterator[Cache.Entry[K, V]]) {

    import scala.collection.JavaConverters._

    def allAsScala() = Future(iterator.asScala.map(_.getValue).toList)
  }

  /** *
    * Implicit class used in daos on insert/upsert or delete, for refresh cache entry
    */

  implicit class FutureToIgnite[A](daoFuture: Future[A]) {

    def cached(replace: Boolean = false): Future[A] = {
      if (cacheEnabled) {
        daoFuture.onComplete { elements =>
          elements match {
            case Success(elems) =>
              List(elems).flatten(List(_).asInstanceOf[List[SpartaEntity]]).foreach { entity =>
                  Try {
                    val entityId = getSpartaEntityId(entity)
                    if (replace)
                      cache.replace(entityId, entity)
                    else
                      cache.putIfAbsent(entityId, entity)
                    entityId
                  } match {
                    case Success(entityId) =>
                      log.debug(s"Updated entity associated to $tableName with Id $entityId")
                    case Failure(e) =>
                      throw e
                  }
                }
            case Failure(e) =>
              log.debug("Error updating cache keys.", e)
          }
        }
      }
      daoFuture
    }

    def remove(id: String*): Future[A] = {
      if (cacheEnabled)
        daoFuture.foreach(_ => cache.removeAllAsync(id.toSet.asJava))
      daoFuture
    }
  }

  /**
    * Ignite async methods return java IgniteFuture, this class convert to Scala future
    */
  implicit class IgniteFutureUtils[T](igniteFuture: IgniteFuture[T]) {

    def toScalaFuture() = {
      val promise = Promise[T]()
      igniteFuture.listen(new IgniteInClosure[IgniteFuture[T]] {
        override def apply(e: IgniteFuture[T]): Unit =
          promise.tryComplete(Try {
            Option(e.get).getOrElse(throw new RuntimeException("Not found in cache"))
          })
      })
      promise.future
    }
  }

}
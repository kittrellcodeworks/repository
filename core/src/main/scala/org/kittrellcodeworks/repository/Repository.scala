package org.kittrellcodeworks.repository

import org.kittrellcodeworks.repository.EntityId.Ops
import org.kittrellcodeworks.repository.query._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
 * The basic Repository knows how to find, count, insert, update and delete domain
 * entities from persistent storage.
 *
 * @author Sam Rushing (rush)
 */
trait Repository {

  /**
   * The domain entity type for which this repository is pertinent.
   */
  type Entity

  /**
   * A domain query type - usually an Algebraic Data Type.
   */
  type Query

  type Id

  protected final type SavableEntity = (Id, Entity)

  /**
   * The underlying database's interface for querying for objects.
   * In Mongo, this would translate to a BSONDocument.
   *
   * Generic repository definitions should not provide this type, but
   * should leave it to the concrete storage-system-specific implementation
   * to define.
   */
  protected type InternalQuery

  protected implicit def ec: ExecutionContext

  /**
   * Internalizes a domain query into one the storage medium understands.
   *
   * Generic repository definitions should not provide this function, but
   * should leave it to the concrete storage-system-specific implementation
   * to define.
   */
  protected implicit def internalize(q: Query): InternalQuery

  protected implicit val entityId: EntityId[Entity, Id] // NOTE: local vals with this name will shadow this implicit

  protected implicit val idGenerator: IdGenerator[Id]

  // -------- internalized which implementations must provide --------

  /**
   * Perists entity changes.
   */
  protected def internalSave(entity: SavableEntity): Future[Unit]

  /**
   * Inserts entities into persistent storage and returns them with new IDs
   */
  protected def internalInsert(entities: Seq[SavableEntity]): Future[Seq[Entity]]

  /**
   * Finds matching objects in persistent storage, and returns them in an Iterator.
   * Use Iterator in order to avoid excessive memory consumption on large result
   * sets.
   */
  protected def internalFind(query: InternalQuery, page: Option[Pagination], sort: Option[Sortable]): Future[Iterator[Entity]]

  /**
   * Removes matching objects from persistent storage.
   */
  protected def internalRemove(query: InternalQuery): Future[Unit]

  /**
   * Counts matching objects in persistent storage.
   */
  protected def internalCount(query: InternalQuery): Future[Long]

  // -------- external domain functionality --------

  /**
   * Persists entity changes.
   */
  def save(entity: Entity): Future[Unit] =
    entity.extractId.fold(Future.failed[Unit](
      new IllegalArgumentException("Repository.save an entity is missing its id.")
    ))(internalSave)

  /**
   * Removes objects in storage that match the domain query's criteria
   */
  def remove(q: Query): Future[Unit] = internalRemove(q)

  /**
   * Counts objects in storage that match the domain query's criteria
   */
  def count(q: Query): Future[Long] = internalCount(q)

  /**
   * Inserts a single entity into persistent storage
   */
  def insertOne(entity: Entity): Future[Entity] = insert(Seq(entity)).map(_.head)

  /**
   * Inserts entities into persistent storage and returns them paired with new IDs
   */
  def insert(entities: Seq[Entity]): Future[Seq[Entity]] =
    if (entities.isEmpty) Future.successful(Nil)
    else internalInsert(entities.map(e => idGenerator.create -> e.extractId.fold(e)(_._2)))

  /**
   * Finds the first object in storage that matches the domain query's criteria
   */
  def findOne(q: Query): Future[Option[Entity]] = find(q) map { i =>
    if (i.hasNext) Some(i.next) else None
  }

  /**
   * Finds objects in storage that match the domain query's criteria
   */
  def find(q: Query): Future[Iterator[Entity]] = q match {
    case PageableQuery(page, sort) => internalFind(q, Some(page), sort)
    case _ => internalFind(q, None, None)
  }

}

object Repository {

  abstract class Instance[E, I, Q] extends Repository {
    final type Entity = E
    final type Id = I
    final type Query = Q
  }

}

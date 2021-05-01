package org.kittrellcodeworks.repository
package mongo

import org.kittrellcodeworks.repository.EntityId.Ops
import org.kittrellcodeworks.repository.mongo.ReactiveMongoRepository._
import org.kittrellcodeworks.repository.query._
import reactivemongo.api._
import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.{ExecutionContext, Future}

/**
 * Mongo Implementation of [[org.kittrellcodeworks.repository.Repository]]
 */
trait ReactiveMongoRepository { self: Repository =>

  final override type InternalQuery = BSONDocument

  def collection: Future[BSONCollection]

  protected def idHandler: BSONHandler[Id]

  protected implicit def ec: ExecutionContext

  protected implicit def entityHandler: BSONDocumentHandler[Entity]

  // these need to be lazy because idHandler will be null until subclass finishes instantiation.
  protected implicit lazy val idFormats: BSONDocumentHandler[Id] = idDocHandler(idHandler)
  protected implicit lazy val pairFormats: BSONDocumentHandler[SavableEntity] = pairHandler(idFormats)

  override def internalFind(query: InternalQuery,
                    page: Option[Pagination],
                    sort: Option[Sortable]): Future[Iterator[Entity]] =
    collection.find[SavableEntity](query, paginateAndSort(page, sort)).map(_.map {
      case (id, e) => e withId id
    })

  override def internalRemove(q: InternalQuery): Future[Unit] = for {
    c ← collection
    _ ← c.delete.one(q)
  } yield ()

  override def internalSave(entity: SavableEntity): Future[Unit] = for {
    c ← collection
    r ← c.update.one(entity._1, entity)(ec, idFormats, pairFormats)
  } yield {
    if (r.nModified == 0) throw EntityIsNewException()
  }

  override def internalInsert(eds: Seq[SavableEntity]): Future[Seq[Entity]] = {
    if (eds.size < 1) Future.successful(Seq.empty)
    else if (eds.size == 1) insertOne(eds.head).map(Seq(_))
    else for {
      c <- collection
      _ <- c.insert.many(eds)
    } yield eds.map(_.combine[Entity, Id])
  }

  @inline private def insertOne(entityData: SavableEntity): Future[Entity] = {
    for {
      c ← collection
      _ ← c.insert.one(entityData)
    } yield entityData._2.withId(entityData._1)
  }

  override protected def internalCount(query: InternalQuery): Future[Long] =
    collection.flatMap(_.count(if (query.isEmpty) None else Some(query)))

}

object ReactiveMongoRepository {

  val allInternalQuery: BSONDocument = BSONDocument.empty

  val objectIdHandler: BSONHandler[String] = BSONHandler.from[String](
    bson => bson.asTry[BSONObjectID].map(_.stringify),
    id => BSONObjectID.parse(id)
  )

  // provided for default String id generation where each Id must parse as a BSONObjectID
  implicit val objectIdGenerator: IdGenerator[String] =
    IdGenerator(BSONObjectID.generate.stringify, BSONObjectID.parse(_).get.stringify)

  // provided for determining a default idGenerator if no implicit is available during creation of a repository.
  implicit def passthroughGenerator[T]: IdGenerator[T] = IdGenerator.passThrough

  def defaultGenerator[T]: IdGenerator[T] = implicitly[IdGenerator[T]]

  def pairHandler[E, I](id: BSONDocumentHandler[I])(implicit f: BSONDocumentHandler[E],
                                                    ev: EntityId[E, I]): BSONDocumentHandler[(I, E)] = {
    locally(ev)
    BSONDocumentHandler.from[(I, E)](
      doc => for {i <- id.readTry(doc); e <- f.readTry(doc)} yield i -> e,
      { case (i, e) => for {d0 <- id.writeTry(i); d1 <- f.writeTry(e)} yield d1 ++ d0 }
    )
  }

  def idDocHandler[I](implicit h: BSONHandler[I]): BSONDocumentHandler[I] =
    BSONDocumentHandler.from[I](_.getAsTry[I]("_id"), id => h.writeTry(id).map(v => BSONDocument("_id" → v)))

  /**
   * A mixin-ready constructor for Mongo Implementations of [[org.kittrellcodeworks.repository.Repository]]
   *
   * @param dbName         Mongo Database Name
   * @param collectionName Mongo Collection Name
   * @param idHandler      The specialized BSON handler for processing entity ids
   * @tparam E Entity type
   * @tparam I Entity ID type
   */
  class Instance[E, I, Q](val dbName: String,
                          val collectionName: String,
                          protected val idHandler: BSONHandler[I])
                         (implicit mongoConnection: MongoConnection,
                          protected val ec: ExecutionContext,
                          protected val entityHandler: BSONDocumentHandler[E],
                          val entityId: EntityId[E, I],
                          val idGenerator: IdGenerator[I],
                          queryWriter: BSONDocumentWriter[Q])
    extends Repository.Instance[E, I, Q] with ReactiveMongoRepository {

    override lazy val collection: Future[BSONCollection] =
      mongoConnection.database(dbName).map(_.collection(collectionName))

    override def internalize(q: Q): BSONDocument = queryWriter.writeTry(q).get
  }

}


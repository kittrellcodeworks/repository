package org.kittrellcodeworks.repository
package mongo

import org.kittrellcodeworks.repository.EntityId.Ops
import org.kittrellcodeworks.repository.upsert._
import reactivemongo.api.bson.BSONDocument
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.Future

/**
 * This mixin provides default functionality for upsertable behavior in mongo.
 */
trait ReactiveMongoUpsert extends Upsert { self: Repository with ReactiveMongoRepository =>

  override protected def internalUpsert(entities: Seq[SavableEntity]): Future[Seq[UpsertResult[Entity]]] = {
    if (entities.isEmpty) Future.successful(Nil)
    else for {
      c ← collection
      docs = entities.map(p => idFormats.writeTry(p._1).get -> entityHandler.writeTry(p._2).get) // throws exceptions, failing the Future.
      v ← updateMany(c, docs)
    } yield {
      val upserted = v.upserted.map(_.index).toSet
      entities.zipWithIndex map {
        case ((id, e), i) => (if (upserted(i)) Inserted else Updated) -> e.withId(id)
      }
    }
  }

  private def updateMany(coll: BSONCollection, docs: Iterable[(BSONDocument, BSONDocument)]) = {
    val update = coll.update(ordered = true)
    val fs = docs.map {
      case (idDoc, entDoc) => update.element(
        q = idDoc,
        u = BSONDocument(f"$$set" -> entDoc),
        upsert = true,
        multi = false)
    }
    Future.sequence(fs).flatMap(update.many(_)) // Future[MultiBulkWriteResult]
  }

}
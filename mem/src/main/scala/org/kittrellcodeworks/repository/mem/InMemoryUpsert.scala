package org.kittrellcodeworks.repository
package mem

import org.kittrellcodeworks.repository.EntityId.Ops
import org.kittrellcodeworks.repository.upsert._

import scala.concurrent.Future

trait InMemoryUpsert extends Upsert { self: Repository with InMemoryRepository =>

  override protected def internalUpsert(entities: Seq[SavableEntity]): Future[Seq[UpsertResult[Entity]]] = {
    if (entities.isEmpty) Future.successful(Nil)
    else Future {
      latch.synchronized {
        entities map {
          case (id, e) =>
            val existed = has(id)
            put(id, Some(e))
            (if (existed) Updated else Inserted) -> e.withId(id)
        }
      }
    }
  }

}
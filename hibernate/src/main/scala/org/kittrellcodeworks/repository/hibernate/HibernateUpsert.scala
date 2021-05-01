package org.kittrellcodeworks.repository
package hibernate

import org.kittrellcodeworks.repository.EntityId.Ops
import org.kittrellcodeworks.repository.upsert.{Inserted, Updated, Upsert, UpsertResult}

import scala.collection.convert.ImplicitConversionsToScala._
import scala.concurrent.Future
import java.{util => ju}

trait HibernateUpsert extends Upsert { self: Repository with HibernateRepository =>

  override protected def internalUpsert(entities: Seq[SavableEntity]): Future[Seq[UpsertResult[Entity]]] = {
    if (entities.isEmpty) Future.successful(Nil)
    else connection.withTransaction { (sess, _) =>
      val arr = if (isIdWrappedInOption) entities.map(e => Some(e._1)) else entities.map(_._1)
      val ids: ju.List[Any] = new ju.ArrayList(arr.size) // java collection required for `IN` searches.
      arr foreach ids.add
      val returnClass: Class[_] = if (isIdWrappedInOption) classOf[Option[Id]] else ids.head.getClass
      val query: InternalQuery = s"E.${entityId.fieldName} IN :entityIds" -> Map("entityIds" -> ids)
      val q0 = buildQuery(s"SELECT E.${entityId.fieldName} FROM ", query, None, sess, returnClass)
      val res = q0.list().toSet
      val existing: Set[Id] =
        if (isIdWrappedInOption) res.map(_.asInstanceOf[Option[Id]].get)
        else res.asInstanceOf[Set[Id]]
      val withIds = entities.map { case (i, e) => (if (existing contains i) Updated else Inserted) -> (e withId i) }
      withIds.foreach(t => sess.saveOrUpdate(t._2))
      sess.flush()
      withIds
    }
  }

}

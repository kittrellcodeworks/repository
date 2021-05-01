package org.kittrellcodeworks.repository.solr

import org.kittrellcodeworks.repository.EntityId.Ops
import org.kittrellcodeworks.repository.Repository
import org.kittrellcodeworks.repository.upsert.{Inserted, Updated, Upsert, UpsertResult}
import org.apache.solr.client.solrj.SolrQuery

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.concurrent.Future

trait SolrUpsert extends Upsert { self: Repository with SolrRepository =>

  override protected def internalUpsert(entities: Seq[SavableEntity]): Future[Seq[UpsertResult[Entity]]] = {
    if (entities.isEmpty) Future.successful(Nil)
    else {
      val p = new SolrQuery().setFields(entityId.fieldName).setRows(entities.size)
      for {
        r0 <- collection.getByIds(entities.map(t => internalizeId(t._1)), p)
        existing = r0.asScala.map(doc => externalizeId(doc.get(entityId.fieldName).toString)).toSet
        _ <- collection.add(entities map internalizeSavable)
      } yield entities.map {
        case (i, e) => (if (existing(i)) Updated else Inserted) -> e.withId(i)
      }
    }
  }


}

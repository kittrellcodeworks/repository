package org.kittrellcodeworks.repository
package solr

import org.kittrellcodeworks.repository.EntityId.Ops
import org.kittrellcodeworks.repository.query.{Asc, Pagination, Sortable}
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.{SolrDocument, SolrException, SolrInputDocument}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait SolrRepository { self: Repository =>

  final type InternalQuery = SolrQuery

  protected val collection: SolrCollection

  // In theory, we could use the Solr DocumentObjectBinder to map SolrDocuments from and to our Entity class,
  // but that would require Java-style getter and setter methods, which we don't want to require, here.
  // So, we'll have our implementations provide conversion functions.

  protected def internalizeId(id: Id): String // Solr IDs are all Strings

  protected def externalizeId(id: String): Id // Solr IDs are all Strings

  protected def internalizeEntity(entity: Entity, id: Option[Id]): SolrInputDocument = {
    val doc = new SolrInputDocument()
    id.foreach(i => doc.addField(entityId.fieldName, i))
    doc
  }

  protected def externalizeEntity(doc: SolrDocument): Entity

  protected def internalizeSavable(entity: SavableEntity): SolrInputDocument =
    internalizeEntity(entity._2, Some(entity._1))

  protected def externalizeEntityWithId(doc: SolrDocument): Entity = {
    val id = Option(doc.get(entityId.fieldName)).map(i => externalizeId(i.toString))
    entityId.withId(externalizeEntity(doc), id)
  }

  // ----- implementation methods -----

  protected def internalSave(entity: SavableEntity): Future[Unit] = {
    val doc = internalizeSavable(entity)
    doc.addField("_version_", 1L) // requires that the document already exist in the db.
    collection.addOne(doc).recoverWith {
      case se: SolrException if se.code == SolrException.ErrorCode.CONFLICT.code =>
        throw EntityIsNewException(se)
    }.map(_ => ())
  }

  protected def internalInsert(entities: Seq[SavableEntity]): Future[Seq[Entity]] =
    collection.add(entities map internalizeSavable).map(_ => entities.map(t => t._2 withId t._1))

  protected def internalFind(query: InternalQuery,
                             page: Option[Pagination],
                             sort: Option[Sortable]): Future[Iterator[Entity]] = {
    page.foreach { p =>
      query.setStart((p.page - 1) * p.limit)
      query.setRows(p.limit)
    }
    sort.foreach { s =>
      query.setSort(if (s.dir == Asc) SolrQuery.SortClause.asc(s.fieldName) else SolrQuery.SortClause.desc(s.fieldName))
    }
    query.setFields("*")
    collection.query(query).map { resp =>
      val results = Option(resp.getResults).map(_.asScala.map(externalizeEntityWithId))
      results.getOrElse(Nil).toIterator
    }
  }

  /**
   * Removes matching objects from persistent storage.
   */
  protected def internalRemove(query: InternalQuery): Future[Unit] = Future {
    collection.deleteByQuery(query.getQuery)
  }

  /**
   * Counts matching objects in persistent storage.
   */
  protected def internalCount(query: InternalQuery): Future[Long] = {
    query.setRows(0)
    collection.query(query).map(_.getResults.getNumFound)
  }

}

object SolrRepository {

  val allInternalQuery: SolrQuery = new SolrQuery("*:*").setRows(Int.MaxValue)

  class Instance[E, I, Q](conn: SolrConnection,
                          collectionName: String,
                          readId: String => I,
                          writeId: I => String,
                          readEnt: SolrDocument => E,
                          writeEnt: (E, SolrInputDocument) => SolrInputDocument)
                         (_internalize: Q => SolrQuery)
                         (implicit protected val ec: ExecutionContext,
                          val entityId: EntityId[E, I],
                          val idGenerator: IdGenerator[I])
    extends Repository.Instance[E, I, Q] with SolrRepository {

    override protected val collection: SolrCollection = conn.collection(collectionName)

    override def internalize(q: Q): InternalQuery = _internalize(q)

    override protected def internalizeId(id: Id): String = writeId(id)

    override protected def externalizeId(id: String): Id = readId(id)

    override protected def internalizeEntity(entity: Entity, id: Option[Id]): SolrInputDocument =
      writeEnt(entity, super.internalizeEntity(entity, id))

    override protected def externalizeEntity(doc: SolrDocument): Entity = readEnt(doc)
  }

}

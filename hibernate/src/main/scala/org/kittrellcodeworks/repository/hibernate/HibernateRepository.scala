package org.kittrellcodeworks.repository
package hibernate

import org.kittrellcodeworks.repository.EntityId.Ops
import org.kittrellcodeworks.repository.query.{Desc, Pagination, Sortable}
import org.hibernate.Session
import org.hibernate.query.{Query => HQuery}

import javax.persistence.{OptimisticLockException, Query => JQuery}
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.reflect.ClassTag

trait HibernateRepository { self: Repository =>

  final type InternalQuery = (String, Map[String, AnyRef])

  protected def connection: HibernateConnection
  protected def entityName: String
  protected def clazz: Class[Entity]
  protected implicit def idClassTag: ClassTag[Id]
  protected def isIdWrappedInOption: Boolean

  protected def internalSave(entity: SavableEntity): Future[Unit] =
    connection.withTransaction { (sess, _) =>
      val e = entity._2 withId entity._1
      sess update e
      sess.flush()
    }.recover {
      case e: OptimisticLockException => throw EntityIsNewException(e)
    }

  // NOTE - if you are using a database-level id generator, override `insert` as well.
  protected def internalInsert(entities: Seq[SavableEntity]): Future[Seq[Entity]] =
    connection.withTransaction { (sess, _) =>
      val withIds = entities.map { case (i, e) => e withId i }
      withIds foreach sess.save
      sess.flush()
      withIds
    }

  protected def internalFind(query: InternalQuery,
                             page: Option[Pagination],
                             sort: Option[Sortable]): Future[Iterator[Entity]] =
    connection.withSession { sess =>
      var q = buildQuery("FROM ", query, sort, sess, clazz)
      page.foreach { p =>
        if (p.page > 1) q = q.setFirstResult((p.page - 1) * p.limit)
        if (p.limit < Int.MaxValue && p.limit > 0) q = q.setFetchSize(p.limit)
      }
      q.list().iterator().asScala
    }

  protected def internalRemove(query: InternalQuery): Future[Unit] =
    connection.withTransaction { (sess, _) =>
      val q = buildQuery("DELETE ", query, None, sess.createQuery)
      q.executeUpdate()
    }

  protected def internalCount(query: InternalQuery): Future[Long] =
    connection.withSession { sess =>
      val q = buildQuery("SELECT count(E) FROM ", query, None, sess, classOf[java.lang.Long])
      q.uniqueResult()
    }

  protected def buildQuery[T](select: String,
                              query: InternalQuery,
                              sort: Option[Sortable],
                              sess: Session,
                              resultClass: Class[T]): HQuery[T] =
    buildQuery(select, query, sort, sess.createQuery(_, resultClass))

  protected def buildQuery[Q <: JQuery](select: String,
                                        query: InternalQuery,
                                        sort: Option[Sortable],
                                        mkQ: String => Q): Q = {
    val qStr = new StringBuilder(select)
    qStr append entityName
    qStr append " E"
    if (query._1.trim.nonEmpty) {
      qStr append " WHERE "
      qStr append query._1
    }
    sort foreach { s =>
      qStr append " ORDER BY "
      qStr append s.fieldName
      qStr append (if (s.dir == Desc) " DESC" else " ASC")
    }
    val q = mkQ(qStr.toString)
    query._2 foreach {
      case (k, v) =>
        q.setParameter(k, v)
    }
    q
  }

}

object HibernateRepository {

  val allInternalQuery: (String, Map[String, AnyRef]) = "" -> Map.empty

  class Instance[E, I, Q](protected val connection: HibernateConnection)
                         (_internalize: Q => (String, Map[String, AnyRef]))
                         (implicit protected val ec: ExecutionContext,
                          val entityId: EntityId[E, I],
                          val idGenerator: IdGenerator[I],
                          ct: ClassTag[E],
                          protected val idClassTag: ClassTag[I])
    extends Repository.Instance[E, I, Q] with HibernateRepository {

    override protected def clazz: Class[E] = ct.runtimeClass.asInstanceOf[Class[E]]

    override protected val entityName: String = connection.getEntityName[E]

    override protected implicit def internalize(q: Q): InternalQuery = _internalize(q)

    override protected val isIdWrappedInOption: Boolean =
      classOf[Option[Id]].isAssignableFrom(ct.runtimeClass.getDeclaredField(entityId.fieldName).getType)
  }

}

package org.kittrellcodeworks.repository.hibernate

import org.kittrellcodeworks.repository.hibernate.types.optionalTypes
import org.hibernate.{Session, SessionFactory, Transaction}
import org.hibernate.boot.{MetadataBuilder, MetadataSources}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class HibernateConnection(metadataSources: MetadataSources,
                          addMetadata: MetadataBuilder => Unit = _ => ())
                         (implicit ec: ExecutionContext) {

  private val sessionFactory: SessionFactory = {
    val mdBuilder = metadataSources.getMetadataBuilder
    optionalTypes.foreach { t =>
      mdBuilder.applyBasicType(t, t.getName)
    }
    addMetadata(mdBuilder)
    mdBuilder.build.buildSessionFactory
  }

  private var closed = false

  private object latch

  def withSession[T](body: Session => T): Future[T] =
    Future(latch.synchronized{
      if (!closed) {
        val session = sessionFactory.openSession()
        try {
          body(session)
        } finally {
          session.close()
        }
      }
      else throw new IllegalStateException("HibernateSession.closed")
    })

  def withTransaction[T](body: (Session, Transaction) => T): Future[T] =
    Future(latch.synchronized{
      if (!closed) {
        val session = sessionFactory.openSession()
        var transaction: Transaction = null
        try {
          transaction = session.beginTransaction()
          val ret = body(session, transaction)
          transaction.commit()
          ret
        } catch {
          case e: Throwable =>
            if (transaction != null) transaction.rollback()
            throw e
        } finally {
          session.close()
        }
      }
      else throw new IllegalStateException("HibernateSession.closed")
    })

  def getEntityName[E](implicit ct: ClassTag[E]): String =
    sessionFactory.getMetamodel.entity(ct.runtimeClass).getName

  def close(): Unit = {
    if (!closed) sessionFactory.close()
    closed = true
  }

}

object HibernateConnection {

  def apply(metadataSources: MetadataSources)(implicit ec: ExecutionContext): HibernateConnection =
    new HibernateConnection(metadataSources)

  def withCustomMetadata(metadataSources: MetadataSources)
                        (addMetadata: MetadataBuilder => Unit)
                        (implicit ec: ExecutionContext): HibernateConnection =
    new HibernateConnection(metadataSources, addMetadata)

}

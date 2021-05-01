package org.kittrellcodeworks.repository

import org.kittrellcodeworks.repository.query.{Pagination, Sortable}

import scala.concurrent.Future

/**
 * Mix this trait in to your [[Repository]] type definition to gain `findAll`, `removeAll` and `countAll behavior.
 *
 * @author Sam Rushing (rush)
 */
trait AllQueries { self: Repository =>

  def findAll(): Future[Iterator[Entity]]

  def removeAll(): Future[Unit]

  def countAll(): Future[Long]

}

object AllQueries {

  // TODO -- Add pagination here?

  /**
   * Mix this trait into your [[Repository]] factory's instantiation call to describe the "All Query" in terms of
   * a domain Query.
   *
   * @author Sam Rushing (rush)
   */
  trait FromDomain extends AllQueries { self: Repository =>

    protected def allQuery: Query

    override def findAll(): Future[Iterator[Entity]] = find(allQuery)

    override def removeAll(): Future[Unit] = remove(allQuery)

    override def countAll(): Future[Long] = count(allQuery)

  }

  /**
   * Mix this trait into your [[Repository]] factory's instantiation call to describe the "All Query" in terms of
   * a database-specific query.
   *
   * @author Sam Rushing (rush)
   */
  trait FromInternal extends AllQueries { self: Repository =>

    protected def allQuery: InternalQuery
    protected def allQueryPaging: (Option[Pagination], Option[Sortable])

    override def findAll(): Future[Iterator[Entity]] = {
      val (p, s) = allQueryPaging
      internalFind(allQuery, p, s)
    }

    override def removeAll(): Future[Unit] = internalRemove(allQuery)

    override def countAll(): Future[Long] = internalCount(allQuery)

  }

}

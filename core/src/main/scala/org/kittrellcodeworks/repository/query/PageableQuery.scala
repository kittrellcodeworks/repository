package org.kittrellcodeworks.repository.query

/**
 * Mix this trait into your domain query, or a subclass thereof, to indicate that results from
 * that query are page-able and, optionally sortable.
 *
 * @author Sam Rushing
 */
trait PageableQuery {

  def pagination: Pagination

  def sortable: Option[Sortable]

}

object PageableQuery {
  /**
   * An extraction function that extracts [[Pagination]] and [[Sortable]] data from domain queries.
   */
  def unapply(q: PageableQuery): Option[(Pagination, Option[Sortable])] = Some((q.pagination, q.sortable))
}

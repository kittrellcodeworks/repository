package org.kittrellcodeworks.repository
package query

/**
 * A Pagination record.
 * @param page An integer describing the current page starting from 1.
 * @param limit The number of Entities to include on each page.
 *
 * @author Sam Rushing
 */
case class Pagination(page: Int, limit: Int) {
  require(page > 0, "page must be greater than 0")
  require(limit > 0, "limit must be greater than 0")
}

object Pagination {
  /**
   * A default value that [[Repository]] implementations can use. It is up to the domain Query definition to determine
   * whether to use this value or not.
   */
  val default: Pagination = Pagination(1, 25)
}



package org.kittrellcodeworks.repository.sample

import org.kittrellcodeworks.repository.query.{PageableQuery, Pagination, Sortable}

/**
 * `EmployeeQuery` is a sample Algebraic Data Type that defines all supported queries into an
 * `EmployeeRepository`. Any `EmployeeRepository` implementation must provide a means to internalize
 * all variations of this ADT into their internal query representations.
 */
sealed trait EmployeeQuery

case class EmployeeById(id: EmployeeId) extends EmployeeQuery

case class EmployeesByManagerId(mgrId: ManagerId, page: Int = 1, limit: Int = Int.MaxValue)
  extends EmployeeQuery
    with PageableQuery {

  override def pagination: Pagination = Pagination(page, limit)

  override def sortable: Option[Sortable] = Some(Sortable("name"))
}

case class AllEmployees(page: Int = 1, limit: Int = Int.MaxValue) extends EmployeeQuery with PageableQuery {
  override def pagination: Pagination = Pagination(page, limit)

  override def sortable: Option[Sortable] = Some(Sortable("id"))
}

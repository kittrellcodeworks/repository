package org.kittrellcodeworks.repository.mem

import org.kittrellcodeworks.repository.sample._
import org.kittrellcodeworks.repository.{AllQueries, RepositorySpec}

import scala.concurrent.ExecutionContext.Implicits.global

class InMemoryRepositorySpec extends RepositorySpec("in-memory") with GenericFieldLabels {

  override def createRepository: EmployeeRepository =
    new InMemoryRepository.Instance[Employee, EmployeeId, EmployeeQuery]({
      case _:AllEmployees => InMemoryRepository.allInternalQuery
      case EmployeeById(id) => (Some(id), _ => true)
      case EmployeesByManagerId(mgrId, _, _) => (None, _.manager.contains(mgrId))
    }) with InMemoryUpsert with AllQueries.FromDomain {
      override protected def allQuery: EmployeeQuery = AllEmployees()
    }

}

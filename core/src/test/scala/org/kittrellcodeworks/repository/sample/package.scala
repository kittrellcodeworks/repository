package org.kittrellcodeworks.repository

import org.kittrellcodeworks.repository.upsert.Upsert

import scala.util.Random

/**
 * The `sample` package defines a simple collection of Employees that have names and, optionally, a manager, which
 * is also an employee.
 */
package object sample {

  // Define types for code readability
  type EmployeeId = String
  type ManagerId = String

  // Define the type of our
  type EmployeeRepository = Repository with Upsert with AllQueries {
    type Entity = Employee
    type Id = EmployeeId
    type Query = EmployeeQuery
  }

  // Define an Id handler for the Entities
  implicit val employeeId: EntityId[Employee, EmployeeId] =
    EntityId.forField[Employee]("id")(_.id)((e, i) => e.copy(id = i))

  // Define an Id Generator for the Entities -- This is only a default,
  //  and not required to be used by all Repository Implementations
  implicit val employeeIdGenerator: IdGenerator[EmployeeId] = IdGenerator(Random.alphanumeric.take(20).mkString)

}

package org.kittrellcodeworks.repository
package hibernate

import org.kittrellcodeworks.repository.sample._
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.scalatest.BeforeAndAfterAll
import org.scalatest.time.{Milliseconds, Seconds, Span}

import scala.concurrent.ExecutionContext.Implicits.global

class HibernateRepositorySpec extends RepositorySpec("hibernate") with BeforeAndAfterAll {

  private var conn: HibernateConnection = _

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(5, Seconds), Span(50, Milliseconds))

  override def createRepository: EmployeeRepository = {
    require(conn != null)
    new HibernateRepository.Instance[Employee, EmployeeId, EmployeeQuery](conn)({
      case _:AllEmployees => HibernateRepository.allInternalQuery
      case EmployeeById(id) => s"E.${employeeId.fieldName}=:empId" -> Map("empId" -> Some(id))
      case EmployeesByManagerId(mgrId, _, _) => s"E.manager=:mgrId" -> Map("mgrId" -> Some(mgrId))
    }) with HibernateUpsert with AllQueries.FromDomain {
      override protected def allQuery: EmployeeQuery = AllEmployees()
    }
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    // A SessionFactory is set up once for an application!
    val registry = new StandardServiceRegistryBuilder().configure.build // configures settings from hibernate.cfg.xml

    try {
      // pretty sure this will break because our entity isn't annotated or a bean
      conn = new HibernateConnection(new MetadataSources(registry))
    } catch {
      case e: Exception =>
        e.printStackTrace()
        // The registry would be destroyed by the SessionFactory, but since we had trouble building the
        // SessionFactory, destroy it manually.
        StandardServiceRegistryBuilder.destroy(registry)
    }
    locally(repo)
  }

  override protected def afterAll(): Unit = {
    if (conn != null) conn.close()
    super.afterAll()
  }

}

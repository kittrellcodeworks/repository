package org.kittrellcodeworks.repository.solr

import org.kittrellcodeworks.repository.sample._
import org.kittrellcodeworks.repository.{AllQueries, RepositorySpec}
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.common.{SolrDocument, SolrInputDocument}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.time.{Milliseconds, Seconds, Span}

import java.nio.file.Paths
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class SolrRepositorySpec extends RepositorySpec("solr") with BeforeAndAfterAll {

  // we have to allow enough time for the inserts to complete
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(50, Milliseconds))

  var conn: SolrConnection = _

  def readEmployee(doc: SolrDocument): Employee = {
    // id field will be read elsewhere
    Employee(
      id = None,
      name = Option(doc.get("name")).map(_.toString).getOrElse(""),
      manager = Option(doc.get("manager")).map(_.toString)
    )
  }

  def writeEmployee(e: Employee, doc: SolrInputDocument): SolrInputDocument = {
    doc.addField("name", e.name)
    e.manager.foreach(m => doc.addField("manager", m))
    doc
  }

  override def createRepository: EmployeeRepository =
    new SolrRepository.Instance[Employee, EmployeeId, EmployeeQuery](
      conn, "employee", identity, identity, readEmployee, writeEmployee)({
      case _:AllEmployees => SolrRepository.allInternalQuery
      case EmployeeById(id) => new SolrQuery(s"id:$id")
      case EmployeesByManagerId(mgrId, _, _) => new SolrQuery(s"manager:$mgrId")
    }) with SolrUpsert with AllQueries.FromDomain {
      override protected def allQuery: EmployeeQuery = AllEmployees()
    }

  override def beforeAll(): Unit = {
    super.beforeAll()
    val path = Paths.get("solr", "testdata", "solr").toAbsolutePath
    System.setProperty("solr.solr.home", path.toString);
    val client = new EmbeddedSolrServer(path, "employee")
    conn = new SolrConnection(client)
    client.deleteByQuery("*:*") // clear the database, in case a previous test run left data in it.
    client.commit()

    val q = new SolrQuery("*:*")
    q.setRows(0)
    client.query("employee", q).getResults.getNumFound shouldBe 0

    locally(repo)
  }

  override def afterAll(): Unit = {
    Await.result(conn.close(), 3.seconds)
    super.afterAll()
  }
}

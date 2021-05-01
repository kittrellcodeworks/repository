package org.kittrellcodeworks.repository.mongo

import org.kittrellcodeworks.repository.mongo.ReactiveMongoRepository.objectIdHandler
import org.kittrellcodeworks.repository.query.{Pagination, Sortable}
import org.kittrellcodeworks.repository.sample.{employeeIdGenerator => _, _}
import org.kittrellcodeworks.repository.{AllQueries, RepositorySpec}
import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.time.{Milliseconds, Seconds, Span}
import reactivemongo.api.bson._
import reactivemongo.api.{AsyncDriver, MongoConnection}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Success

class ReactiveMongoRepositorySpec extends RepositorySpec("mongo") with MongoEmbedDatabase with BeforeAndAfterAll {

  var mongoProps: MongodProps = _
  var driver: AsyncDriver = _

  val conf: Config = ConfigFactory.load()

  // we have to allow enough time for the mongod process to start up
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(50, Milliseconds))

  implicit val eh: BSONDocumentHandler[Employee] = shapeless.cachedImplicit

  // NOTE: Use the
  implicit val qh: BSONDocumentWriter[EmployeeQuery] = BSONDocumentWriter.from[EmployeeQuery]{
    case EmployeeById(id) =>
      // NOTE since we're using BSONObjectIDs instead of other types for our IDs, we can't create query values easily
      objectIdHandler.writeTry(id).map(bson => BSONDocument("_id" -> bson))
    case EmployeesByManagerId(id, _, _) =>
      // Here, we're storing the manager id reference as a string, so our query construction is simpler.
      // This is NOT ideal, in that your refs should be the same type as your IDs, but this is fine for testing,
      // and illustrates how to build queries when only implicitly-resolvable bsonHandlers are required.
      Success(BSONDocument("manager" -> id)) // <-- look how clean that is!
    case AllEmployees(_, _) =>
      Success(ReactiveMongoRepository.allInternalQuery)
  }

  implicit lazy val mc: MongoConnection =
    Await.result(for {
      parsedUri <- MongoConnection.fromString("mongodb://localhost:12345")
      con <- driver.connect(parsedUri)
    } yield con, Duration.Inf)

  import ReactiveMongoRepository.objectIdGenerator

  override def createRepository: EmployeeRepository =
    new ReactiveMongoRepository.Instance[Employee, EmployeeId, EmployeeQuery](
      "db", "emp", objectIdHandler
    ) with ReactiveMongoUpsert with AllQueries.FromInternal {

      override protected val allQuery: InternalQuery = ReactiveMongoRepository.allInternalQuery
      override protected val allQueryPaging: (Option[Pagination], Option[Sortable]) = None -> None

    }

  override def beforeAll(): Unit = {
    super.beforeAll()
    mongoProps = mongoStart()
    driver = AsyncDriver(conf)
    locally(repo)
  }

  override def afterAll(): Unit = {
    Await.result(driver.close(), Duration.Inf)
    mongoStop(mongoProps)
    super.afterAll()
  }

}

# Kittrell Codeworks DB Repository Framework

The purpose of this framework is to provide consistent domain-driven,
backend-agnostic entry points into data storage and entity management.
To do this, it provides a model for implementing the "Repository Design
Pattern" in scala.

## What is the Repository Design Pattern?

In short, this pattern:

- separates the domain from database-backend, implementation code.
- keeps all database-specific code sequestered in a well-defined location
  
This yields code that is:

- easier to maintain
- easier to swap out database-backends if necessary
- easier to test with mocked or in-memory instances.

## Definitions

**Domain**: The domain is our application's business-logic and models. It is,
basically, the topic or subject of the app.

**Entity**: An model object that the application must persist to storage.

**Query**: A description of a subset of a collection of Entities.

**Id**: The primary, unique (potentially compound) identifier of an entity.

**Repository**: A means of managing a collection of a single type of Entity.

## How CM-DB Repository works

An application should:

1. *Define the Entity class*, preferably, as an un-annotated scala case class
   that contains an Id.
   
   For example:
   
   ```scala
   case class Employee(id: Option[String],
                       name: String,
                       manager: Option[String])
   ```

2. *Define the Queries that will be used in conjunction with this Entity* as a
  scala Algebraic Data Type.
   
   For example:
   
   ```scala
   sealed trait EmployeeQuery
   case class EmployeeById(id: EmployeeId) extends EmployeeQuery
   case class EmployeesByManagerId(mgrId: ManagerId) extends EmployeeQuery
   case object AllEmployees extends EmployeeQuery
   ```

3. *Define a means of extracting an Id from the Entity* as an instance of `EntityId`.
   
   EntityId prodives a generic functional constructor in the form of:

   ```scala
   implicit val employeeId: EntityId[Employee, String] =
     EntityId.forField[Employee]("id")(_.id)((e, i) => e.copy(id = i))
   ```

4. *Define a means of generating new Ids for the Entity* as an instance of `IdGenerator`.
   These are often tied to database-specific implementations, but a generic IdGenerator
   could look like this:

   ```scala
   implicit val employeeIdGenerator: IdGenerator[String] =
     IdGenerator(Random.alphanumeric.take(20).mkString)
   ```
   
   or, if the Id values require validation, it could look something like:

   ```scala
   implicit val uuidIdGenerator: IdGenerator[String] =
     IdGenerator(UUID.randomUUID().toString, {s => UUID.fromString(s); s })
   ```

5. *Define a type for the Repository* that mixes in any additional desired behavior.
   This `type` will be the **only** way that your application references instances of
   the Repository in any class or module.

   For example:

   ```scala
   type EmployeeRepository = Repository with AllQueries {
     type Entity = Employee
     type Id = EmployeeId
     type Query = EmployeeQuery
   }
   ```

6. *Add a dependency to **one** of the implementation-specific cm-db artifacts*.

   For example:

   ```scala
   libraryDependencies += "org.kittrellcodeworks" %% "repo-hibernate" % "0.1-SNAPSHOT"

   // optionally:
   libraryDependencies += "org.kittrellcodeworks" %% "repo-mem" % "0.1-SNAPSHOT" % Test
   ```

7. *Create a factory module* for your Repository that instantiates your repository
   with your chosen database backend and handles your Entity, Id and Query mappings. 

   For example:
   
  ```scala
   object EmployeeRepositoryFactory {
     def create( conn: HibernateConnection ): EmployeeRepository =
        new HibernateRepository.Instance[Employee, EmployeeId, EmployeeQuery](conn)({
           case _:AllEmployees => HibernateRepository.allInternalQuery
           case EmployeeById(id) => s"E.${employeeId.fieldName}=:empId" -> Map("empId" -> Some(id))
           case EmployeesByManagerId(mgrId, _, _) => s"E.manager=:mgrId" -> Map("mgrId" -> Some(mgrId))
        }) with AllQueries.FromDomain {
           override protected def allQuery: EmployeeQuery = AllEmployees()
        }
   }
   ```

8. *Instantiate the Repository* at application initialization.

   For example:

   ```scala
   val hibernateConnection: HibernateConnection = ...
   val employees: EmployeeRepository = EmployeeRepositoryFactory.create(conn)
   ```

9. Inject the instance into constructors of objects that need it as usual.
   
   **DO NOT** create new instances of the repository in every class that
   needs access to the database.

10. Make requests of the repository in client code using the domain-defined Query.

   For example:

   ```scala
   for {
     dave <- employees insertOne Employee(None, "Dave")
     bob <- employees insertOne Employee(None, "Bob", dave.id)
     davesSubCount <- employees count EmployeesByManagerId(dave.id)
   } yield davesSubCount // returns 1L
   ```

## Currently supported database backends

- Solr (`repo-solr`)
- MongoDB (`repo-mongo`)
- Hibernate (`repo-hibernate`)
- In-Memory database (`repo-mem`)

The in-memory database is designed for unit testing with a fully-functional Repository
when mocking single methods is not enough.

## Caveats

- This version of CM-DB makes no attempt to cache or correlate entity objects.

- The Hibernate implementation, due to specifics of the underlying data persistence layer,
  requires extra annotations and a no-arg constructor on Entity classes.

- In-memory and MongoDB implementations use `shapeless` to provide compile-time generics
  for accessing entity properties and creating entities.

- This framework intentionally avoids external dependencies such as `cats` or `scalaz`,
  opting, instead, to return failed scala Futures upon errors.
  
- At the time of the writing of this documentation, results of any sorted query may only
  be sorted by a single field.

## Further Reading
- [Patterns of Enterprise Application Architecture: Repository](https://martinfowler.com/eaaCatalog/repository.html)
- [DevIQ: Repository Pattern](https://deviq.com/design-patterns/repository-pattern)
- [Per-Erik Bergman: Repository Design Pattern](https://medium.com/@pererikbergman/repository-design-pattern-e28c0f3e4a30)
- [Nicolas Rinaudo: Scala Best Practices - Algebraic Data Types](https://nrinaudo.github.io/scala-best-practices/definitions/adt.html)
- [Miles Sabin: Shapeless](https://github.com/milessabin/shapeless)

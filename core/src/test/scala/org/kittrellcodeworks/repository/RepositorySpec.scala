package org.kittrellcodeworks.repository

import org.kittrellcodeworks.repository.sample._
import org.kittrellcodeworks.repository.upsert.{Inserted, Updated}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.io.Source

abstract class RepositorySpec(repoType: String) extends WordSpec with Matchers with ScalaFutures {

  def createRepository: EmployeeRepository

  lazy val repo: EmployeeRepository = createRepository

  var idMap: Map[String, EmployeeId] = Map.empty
  val employees: List[Employee] = Employee.readFrom(Source.fromResource("employees.txt")).toList
  val e1: Employee = employees.head
  var e1Id: EmployeeId = _
  var eXId: EmployeeId = _
  val e1subs: Seq[Employee] = employees.tail.takeWhile(_.manager == e1.id) // manager id not mapped!
  val eXsubs: Seq[Employee] = employees.reverse.takeWhile(_.manager == employees.last.manager).reverse

  def updateMgr(e: Employee): Employee = e.copy(manager = e.manager.map(idMap))

  var passedDeleteEmpX = false

  s"A $repoType Repository" should {
    "be able to insert entities" in {
      // insert one
      whenReady(repo.insertOne(e1)) { e =>
        idMap += e1.id.get -> e.id.get
        e1Id = e.id.get
      }

      // insert many - break the inserts up by manager ONLY because we need to make sure that what we are
      // inserting already has valid manager IDs.

      def doIns(subs: List[Employee]): Unit = if (subs.nonEmpty) {
        whenReady(repo.insert(subs.map(updateMgr))) { inserted =>
          subs.map(_.id.get).zip(inserted) foreach {
            case (in, e) =>
              idMap += in -> e.id.get
          }
        }
      }

      @tailrec def loop(man: EmployeeId, buf: List[Employee], rem: List[Employee]): Unit = {
        if (rem.isEmpty) {
          if (buf.nonEmpty) doIns(buf)
        }
        else if (rem.head.manager.get != man) {
          doIns(buf)
          loop(rem.head.manager.get, rem.head :: Nil, rem.tail)
        }
        else loop(man, rem.head :: buf, rem.tail)
      }
      loop(e1Id, Nil, employees.tail)
      eXId = idMap(employees.last.manager.get)
    }

    "be able to list all entities" in {
      whenReady(repo.find(AllEmployees())) { empIt =>
        val emps = empIt.map(_.id.get).toSet
        emps shouldBe idMap.values.toSet
        emps should contain(e1Id)
        emps should contain(eXId)
      }
    }

    "be able to fetch an entity by its id" in {
      whenReady(repo.findOne(EmployeeById(e1Id))) { eOpt =>
        eOpt shouldBe defined
        val e = eOpt.get
        e.name shouldBe e1.name
        e.manager shouldBe empty
      }
    }

    "be able to query for entities" in {
      whenReady(repo.find(EmployeesByManagerId(e1Id))) { esI =>
        val es = esI.toList
        es should have size e1subs.size
        //println(s"idMap:\n  " + idMap.map{ case(k, v) => k + " -> " + v}.mkString("\n  "))
        val expected = e1subs.map(e => e.copy(id = e.id.map(idMap), manager = Some(e1Id)))
        es should contain theSameElementsAs expected

        es.map(_.name) should contain theSameElementsInOrderAs e1subs.map(_.name).sorted
      }
    }

    "be able to count entities matching a query" in {
      whenReady(repo.count(EmployeeById(eXId))) { c =>
        c shouldBe 1L
      }
      whenReady(repo.count(EmployeesByManagerId(eXId, 1, 1))) { c =>
        c shouldBe eXsubs.size.toLong
      }
      whenReady(repo.count(AllEmployees(1, 0))) { c =>
        c shouldBe employees.size.toLong
      }
    }

    "be able to update an entity" in {
      val update = Employee(Some(e1Id), "Ambergrave Stiles", None)
      whenReady(repo.save(update).flatMap(_ => repo.findOne(EmployeeById(e1Id)))) {
        _ should contain(update)
      }
    }

    "be able to delete entities matching a query" in {
      // NOTE: some databases return from deletes prior to fully committing the changes. So, we have to
      // wait a little bit for them to finish clearing out the detritus.
      val q = EmployeesByManagerId(eXId, 1, 1)
      whenReady(repo.remove(q).flatMap{_ => Thread.sleep(100); repo.count(q)}) { c =>
        c shouldBe 0
      }
      val q1 = EmployeeById(eXId)
      whenReady(repo.remove(q1).flatMap{_ => Thread.sleep(100); repo.count(q1)}) { c =>
        c shouldBe 0
      }
      passedDeleteEmpX = true
    }

    "not be able to update a new entity" in {
      val update = Employee(Some(eXId), "Rambo", None)
      assertThrows[EntityIsNewException] {
        Await.result(repo.save(update), 3.seconds)
        passedDeleteEmpX = false
      }
    }

  }

  s"A $repoType Repository with Upsert" should {
    "be able to upsert an entity that already exists" in {
      whenReady(repo.findOne(EmployeeById(e1Id))) { eOpt =>
        eOpt shouldBe defined
      }
      val update = Employee(Some(e1Id), "Leeroy Jenkins", None)
      whenReady(repo.upsertOne(update)) {
        case (action, e) =>
          e shouldBe update
          whenReady(repo.findOne(EmployeeById(e1Id))) {
            _ shouldBe Some(update)
          }
          action shouldBe Updated
      }
    }

    "not be able to upsert an entity that is new with no ID" in {
      val update = Employee(None, "Leeroy Jenkins", None)
      assertThrows[EntityIsMissingIdException] {
        Await.result(repo.upsertOne(update), 3.seconds)
      }
    }

    "be able to upsert an entity that is new with a specified ID" in {
      assume(passedDeleteEmpX, s"A previous test regarding the id [$eXId] failed.")
      val update = Employee(Some(eXId), "Elon Musk", None)

      whenReady(repo.upsertOne(update)) {
        case (action, e) =>
          action shouldBe Inserted
          e shouldBe update
          whenReady(repo.findOne(EmployeeById(eXId))) {
            _ shouldBe Some(update)
          }
      }
    }
  }

  s"A $repoType Repository with AllQueries" should {
    // lazy because this needs to execute at test time, not class-instantiation.
    lazy val expected =
      employees
        .map { e =>
          e.id.map(idMap) match {
            case Some(i) if i == e1Id => Employee(Some(e1Id), "Leeroy Jenkins", None)
            case Some(i) if i == eXId => Employee(Some(eXId), "Elon Musk", None)
            case i => Employee(i, e.name, e.manager.map(idMap))
          }
        }
        .filterNot(e => e.manager.contains(eXId))

    "be able to list all entities" in {
      whenReady(repo.findAll()) { es =>
        es.toList should contain theSameElementsAs expected
      }
    }

    "be able to count all entities" in {
      whenReady(repo.countAll()) { c =>
        c shouldBe expected.size.toLong
      }
    }

    "be able to remove all entities" in {
      whenReady(repo.removeAll().flatMap{_ => Thread.sleep(100); repo.countAll()}) { c =>
        c shouldBe 0L
      }
    }
  }

}

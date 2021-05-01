package org.kittrellcodeworks.repository
package mem

import org.kittrellcodeworks.repository.EntityId.Ops
import org.kittrellcodeworks.repository.mem.InMemoryRepository.optionalAnyOrd
import org.kittrellcodeworks.repository.query.{Asc, Pagination, SortDir, Sortable}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

trait InMemoryRepository { self: Repository =>

  final override type InternalQuery = (Option[Id], Entity => Boolean)

  private var db = Map.empty[Id, Entity]
  protected val latch = new Object

  protected implicit def fieldLabels: FieldLabels[Entity]

  protected def put(id: Id, entity: Option[Entity]): Unit = latch.synchronized {
    db = entity.fold(db - id)(e => db + (id -> e))
  }

  protected def get(id: Id): Option[Entity] = db.get(id)

  protected def has(id: Id): Boolean = db.contains(id)

  protected def internalSave(entity: SavableEntity): Future[Unit] = {
    val (i, e) = entity
    if (!db.contains(i)) Future.failed(EntityIsNewException())
    else Future.successful(put(i, Some(e)))
  }

  /**
   * Inserts entities into persistent storage and returns them with new IDs
   */
  protected def internalInsert(entities: Seq[SavableEntity]): Future[Seq[Entity]] =
    Future.successful(entities map { case (i, e) => put(i, Some(e)); e withId i })

  protected def doSort(entities: Iterable[(Id, Entity)], fieldName: String, dir: SortDir): Iterable[(Id, Entity)] =
    entities.toSeq.sortBy(_._2 getField fieldName)(if (dir == Asc) optionalAnyOrd else optionalAnyOrd.reverse)

  /**
   * Finds matching objects in persistent storage, and returns them in an Iterator.
   * Use Iterator in order to avoid excessive memory consumption on large result
   * sets.
   */
  protected def internalFind(query: InternalQuery,
                             page: Option[Pagination],
                             sort: Option[Sortable]): Future[Iterator[Entity]] = Future {
    val (idOpt, predicate) = query
    val r0 = idOpt.fold(db.toIterable)(id => db.get(id).map(id -> _)).filter(t => predicate(t._2))
    val r1 = sort.fold(r0)(s => doSort(r0, s.fieldName, s.dir))
    page.fold(r1)(p => r1.slice((p.page - 1) * (p.limit - 1), p.page * p.limit)).map {
      case (id, e) => e withId id
    }.iterator
  }

  /**
   * Removes matching objects from persistent storage.
   */
  protected def internalRemove(query: InternalQuery): Future[Unit] = Future {
    val (idOpt, predicate) = query
    latch.synchronized {
      idOpt match {
        case Some(id) => if (db.get(id).exists(predicate)) db -= id
        case None => db = db.filterNot(e => predicate(e._2))
      }
    }
  }

  /**
   * Counts matching objects in persistent storage.
   */
  protected def internalCount(query: InternalQuery): Future[Long] = {
    val (idOpt, predicate) = query
    Future.successful(idOpt.fold(db.values)(db.get).count(predicate).toLong)
  }

}

object InMemoryRepository {

  val optionalAnyOrd: Ordering[Option[Any]] = (x: Option[Any], y: Option[Any]) => {
    if (x == y) 0
    else if (x.isEmpty) -1
    else if (y.isEmpty) 1
    else (x.get, y.get) match {
      case (a: Int, b: Int) => Ordering.Int.compare(a, b)
      case (a: Long, b: Long) => Ordering.Long.compare(a, b)
      case (a: Byte, b: Byte) => Ordering.Byte.compare(a, b)
      case (a: Char, b: Char) => Ordering.Char.compare(a, b)
      case (a: String, b: String) => Ordering.String.compare(a, b)
      case (a: Boolean, b: Boolean) => Ordering.Boolean.compare(a, b)
      case (a: Short, b: Short) => Ordering.Short.compare(a, b)
      case (a: Float, b: Float) => Ordering.Float.compare(a, b)
      case (a: Double, b: Double) => Ordering.Double.compare(a, b)
      case (a: BigInt, b: BigInt) => Ordering.BigInt.compare(a, b)
      case (a: BigDecimal, b: BigDecimal) => Ordering.BigDecimal.compare(a, b)
      case _ => 0
    }
  }

  val allInternalQuery: (Option[Nothing], Any => Boolean) = None -> (_ => true)

  class Instance[E, I, Q](_internalize: Q => (Option[I], E => Boolean))
                         (implicit protected val ec: ExecutionContext,
                          protected val fieldLabels: FieldLabels[E],
                          val entityId: EntityId[E, I],
                          val idGenerator: IdGenerator[I])
    extends Repository.Instance[E, I, Q] with InMemoryRepository {

    override protected implicit def internalize(q: Q): (Option[I], E => Boolean) = _internalize(q)
  }

}

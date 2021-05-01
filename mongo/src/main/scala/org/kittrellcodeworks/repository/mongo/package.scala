package org.kittrellcodeworks.repository

import org.kittrellcodeworks.repository.query._
import reactivemongo.api.Cursor
import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection

import java.time.{ZoneId, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

package object mongo
  extends GenericBsonHandlers {

  type QueryMutation = BSONCollection#QueryBuilder => BSONCollection#QueryBuilder
  // we can't make this implicit because then it would get in the way of the default BSONStringHandler
  val objectIDHandler: BSONHandler[String] =
    BSONHandler.from[String](_.asTry[String], BSONObjectID.parse)

  def paginateAndSort(pOpt: Option[Pagination], sOpt: Option[Sortable]): (QueryMutation, Int) = {
    var m: QueryMutation = identity
    var limit = Int.MaxValue

    pOpt foreach { p =>
      m = m.andThen(_.skip((p.page - 1) * p.limit))
      if (p.limit > 0) limit = p.limit
    }
    sOpt flatMap SortableWriter.writeOpt foreach { s => m = m.andThen(_.sort(s)) }

    m -> limit
  }

  protected def format[C](r: BSONDocument ⇒ C, w: C ⇒ BSONDocument): BSONDocumentHandler[C] =
    BSONDocumentHandler(r, w)

  // leave it to the Repository implementations to provide an implicit ZoneId
  implicit def zonedDateTimeHandler(implicit zoneId: ZoneId): BSONHandler[ZonedDateTime] =
    bsonZonedDateTimeHandler(zoneId)

  implicit object SortableWriter extends BSONDocumentWriter[Sortable] {
    override def writeTry(sortable: Sortable): Try[BSONDocument] =
      Success(BSONDocument(sortable.fieldName → (if (sortable.dir == Asc) 1 else -1)))
  }

  implicit object SortableOptionWriter extends BSONDocumentWriter[Option[Sortable]] {
    override def writeTry(o: Option[Sortable]): Try[BSONDocument] =
      o map SortableWriter.writeTry getOrElse Success(BSONDocument.empty)
  }

  protected def handler[C](r: BSONValue ⇒ C, w: C ⇒ BSONValue): BSONHandler[C] =
    BSONHandler(r, w)

  implicit class EnrichedFutureCollection(val c: Future[BSONCollection]) extends AnyVal {
    def find[E: BSONDocumentReader](query: BSONDocument, opts: (QueryMutation, Int))
                                   (implicit ec: ExecutionContext): Future[Iterator[E]] =
      c.flatMap { collection =>
        opts._1(collection.find(query))
          .cursor[E]()
          .collect[Iterator](opts._2, Cursor.ContOnError[Iterator[E]]())
      }
  }

}

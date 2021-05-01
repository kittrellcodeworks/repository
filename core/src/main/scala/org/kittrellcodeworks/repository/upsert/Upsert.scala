package org.kittrellcodeworks.repository.upsert

import org.kittrellcodeworks.repository.EntityId.Ops
import org.kittrellcodeworks.repository.{EntityIsMissingIdException, Repository}

import scala.concurrent.Future

/**
 * Mix in this trait to the domain's repository type definition to indicate that any implementation should support
 * "upsert" functionality.
 *
 * @author Sam Rushing (rush)
 */
trait Upsert { self: Repository =>

  protected def internalUpsert(entities: Seq[SavableEntity]): Future[Seq[UpsertResult[Entity]]]

  /**
   * Updates an entity if it already exists in storage, or inserts a new one if not.
   */
  def upsertOne(entity: Entity): Future[UpsertResult[Entity]] = upsert(Seq(entity)).map(_.head)

  /**
   * Updates entities if they already exists in storage, or inserts new ones if not.
   */
  def upsert(entities: Seq[Entity]): Future[Seq[UpsertResult[Entity]]] =
    internalUpsert(entities.map(_.extractId.getOrElse(throw EntityIsMissingIdException())))

}

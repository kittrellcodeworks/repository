package org.kittrellcodeworks.repository

import java.util.UUID
import scala.util.{Success, Try}

/**
 * Determines how a Repository generates and validates Id values for new Entities.
 *
 * Depending on the database-backend, it may be appropriate to share this object among all [[Repository]] instances, or
 * to maintain separate instances for each [[Repository]]
 *
 * @author Sam Rushing (rush)
 */
trait IdGenerator[IdType] {

  /**
   * Creates a new Id value. This could be randomized, sequential, or business-logic-driven.
   */
  def create: IdType

  /**
   * Validates an Id value. This is generally only necessary when the Id type may include values that are not valid
   * (for example, if Id is String but must conform to UUID syntax).
   */
  def validate(rawId: IdType): Try[IdType] = Success(rawId)

}

object IdGenerator {

  // A functional constructor for an IdGenerator that performs no validation
  def apply[I](gen: => I): IdGenerator[I] = new IdGenerator[I] {
    override def create: I = gen
  }

  // A functional constructor for an IdGenerator that performs validation
  def apply[I](gen: => I, v: I => I): IdGenerator[I] = new IdGenerator[I] {
    override def create: I = gen

    override def validate(rawId: I): Try[I] = Try(v(rawId))
  }

  /**
   * An `IdGenerator` that cannot create new values and performs no validation.
   */
  def passThrough[IdType]: IdGenerator[IdType] = PassThrough.asInstanceOf[IdGenerator[IdType]]

  private object PassThrough extends IdGenerator[Any] {
    override def create: Any =
      throw new IllegalArgumentException("IdGenerator.PassThrough.create entity must supply id")
  }

  // provided for default UUID id generation where each Id must be a UUID
  implicit val uuidIdGenerator: IdGenerator[UUID] = IdGenerator(UUID.randomUUID)

}

package org.kittrellcodeworks.repository

/**
 * Thrown by [[Repository.internalSave]] when a supplied entity does not already exist in the database.
 *
 * @author Sam Rushing (rush)
 */
class EntityIsNewException(msg: String, cause: Throwable) extends IllegalArgumentException(msg, cause)

object EntityIsNewException {
  @inline def apply(cause: Throwable = null): EntityIsNewException =
    new EntityIsNewException("Repository.save entity is new", cause)
}

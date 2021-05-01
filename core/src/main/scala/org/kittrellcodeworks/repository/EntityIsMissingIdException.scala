package org.kittrellcodeworks.repository

/**
 * Thrown by [[upsert.Upsert.upsert]] when a supplied entity is missing an Id.
 *
 * @author Sam Rushing (rush)
 */
class EntityIsMissingIdException(msg: String) extends IllegalArgumentException(msg)

object EntityIsMissingIdException {
  @inline def apply(): EntityIsMissingIdException = new EntityIsMissingIdException("Repository.upsert entity is missing id")
}

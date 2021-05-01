package org.kittrellcodeworks.repository

/**
 * The EntityId typeclass correlates an Entity type with its Id type and specifies the name of the field
 * on the Entity on which the Id is stored.
 * @tparam Entity The Entity type for your [[Repository]]
 * @tparam Id The Id type for your [[Repository]]
 *
 * @author Sam Rushing (rush)
 */
trait EntityId[Entity, Id] {

  /**
   * the name of the field on the Entity which holds its Id
   */
  def fieldName: String

  /**
   * Returns the id of the entity, or None if the entity has no id.
   */
  def unapply(entity: Entity): Option[Id]


  /**
   * Returns a copy of the entity with the specified id. If `id` is None, then removes the id from the entity.
   */
  def withId(entity: Entity, id: Option[Id]): Entity

  /**
   * Separates an Entity from its ID in order to allow it to be saved in a storage-medium-specific field, if necessary.
   *
   * @return A tuple of the entity's id and a copy of the entity with the id removed.
   */
  def extractId(entity: Entity): Option[(Id, Entity)] =
    unapply(entity).map(_ -> withId(entity, None))

  /**
   * Pairs the entity with it's id, generating a new id if it doesn't already have one.
   */
  final def makeSavable(entity: Entity)(implicit idGenerator: IdGenerator[Id]): (Id, Entity) =
    extractId(entity) getOrElse (idGenerator.create -> entity)

}

object EntityId {

  private class EntityIdImpl[E, I](val fieldName: String, get: E => Option[I], set: (E, Option[I]) => E)
    extends EntityId[E, I] {
    override def unapply(entity: E): Option[I] = get(entity)
    override def withId(entity: E, id: Option[I]): E = set(entity, id)
  }

  class EntityIdBuilder[E](val fieldName: String) extends AnyVal {
    def apply[I](getter: E => Option[I])(setter: (E, Option[I]) => E): EntityId[E, I] =
      new EntityIdImpl(fieldName, getter, setter)
  }

  def forField[E](fieldName: String): EntityIdBuilder[E] = new EntityIdBuilder[E](fieldName)

  def apply[E, I](implicit i: EntityId[E, I]): EntityId[E, I] = i

  implicit class Ops[E](val entity: E) extends AnyVal {

    def idField(implicit i: EntityId[E, _]): String = i.fieldName

    def id[I](implicit i: EntityId[E, I]): Option[I] = i unapply entity

    def extractId[I](implicit i: EntityId[E, I]): Option[(I, E)] = i extractId entity

    def makeSavable[I](implicit i: EntityId[E, I], idGen: IdGenerator[I]): (I, E) = i makeSavable entity

    def combine[EE, I](implicit ev: E =:= (I, EE), i: EntityId[EE, I]): EE = {
      val (id, e) = ev(entity)
      e withId id
    }

    def withId[I](id: I)(implicit i: EntityId[E, I]): E = i.withId(entity, Option(id))

  }

}

package org.kittrellcodeworks.repository.mem

import org.kittrellcodeworks.repository.util.IsNotOpt
import shapeless._
import shapeless.labelled.FieldType

/**
 * Provides an abstract means of extracting values from an entity by field name.
 */
trait FieldLabels[E] {
  def get(entity: E, field: String): Option[Any]
}

object FieldLabels {

  case class Derived[T](fl: FieldLabels[T])

  implicit val hnilFieldLabels: Derived[HNil] = Derived((_: HNil, _: String) => None)

  implicit def optionalHconsFieldLabels[K <: Symbol, H, T <: HList](implicit wk: Witness.Aux[K],
                                                                    tf: Lazy[Derived[T]]
                                                                   ): Derived[FieldType[K, Option[H]] :: T] = {
    val key = wk.value.name
    Derived((entity: FieldType[K, Option[H]] :: T, field: String) =>
      if (field == key) entity.head else tf.value.fl.get(entity.tail, field))
  }

  implicit def hconsFieldLabels[K <: Symbol, H, T <: HList](implicit wk: Witness.Aux[K],
                                                            tf: Lazy[Derived[T]],
                                                            ev: IsNotOpt[H]
                                                           ): Derived[FieldType[K, H] :: T] = {
    val key = wk.value.name
    locally(ev)
    Derived((entity: FieldType[K, H] :: T, field: String) =>
      if (field == key) Some(entity.head) else tf.value.fl.get(entity.tail, field))
  }

  implicit def deriveFieldLabels[E, R](implicit gen: LabelledGeneric.Aux[E, R], f: Lazy[Derived[R]]): Derived[E] =
    Derived((entity: E, field: String) => f.value.fl.get(gen.to(entity), field))

  def apply[E](implicit f: FieldLabels[E]): FieldLabels[E] = f

}

trait GenericFieldLabels {

  implicit def deriveFieldLabels[E](implicit derived: Lazy[FieldLabels.Derived[E]]): FieldLabels[E] =
    derived.value.fl

}

object GenericFieldLabels extends GenericFieldLabels

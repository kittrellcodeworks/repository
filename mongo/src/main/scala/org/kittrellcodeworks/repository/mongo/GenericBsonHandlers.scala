package org.kittrellcodeworks.repository.mongo

import org.kittrellcodeworks.repository.util.IsNotOpt
import reactivemongo.api.bson._
import shapeless._
import shapeless.labelled.{FieldType, field}

trait DerivedBSONFormats[E] {
  def read(doc: BSONDocument): E

  def write(entity: E): List[ElementProducer]
}

object DerivedBSONFormats {

  implicit object hnilHandler extends DerivedBSONFormats[HNil] {
    override def read(doc: BSONDocument): HNil = HNil

    override def write(entity: HNil): List[ElementProducer] = Nil
  }

  implicit def optionalHconsHandler[K <: Symbol, H, T <: HList](implicit r: BSONReader[H],
                                                                w: BSONWriter[H],
                                                                wk: Witness.Aux[K],
                                                                tf: Lazy[DerivedBSONFormats[T]]
                                                               ): DerivedBSONFormats[FieldType[K, Option[H]] :: T] = {
    val key = wk.value.name
    new DerivedBSONFormats[FieldType[K, Option[H]] :: T] {
      override def read(doc: BSONDocument): FieldType[K, Option[H]] :: T =
        field[K](doc.getAsTry[H](key).toOption) :: tf.value.read(doc)

      override def write(hlist: FieldType[K, Option[H]] :: T): List[ElementProducer] =
        (key → (hlist.head: Option[H])) :: tf.value.write(hlist.tail)
    }
  }

  implicit def hconsHandler[K <: Symbol, H, T <: HList](implicit r: BSONReader[H],
                                                        w: BSONWriter[H],
                                                        wk: Witness.Aux[K],
                                                        tf: Lazy[DerivedBSONFormats[T]],
                                                        ev: IsNotOpt[H]
                                                       ): DerivedBSONFormats[FieldType[K, H] :: T] = {
    locally(ev)
    val key = wk.value.name
    new DerivedBSONFormats[FieldType[K, H] :: T] {
      override def read(doc: BSONDocument): FieldType[K, H] :: T =
        field[K](doc.getAsTry[H](key).get) :: tf.value.read(doc)

      override def write(hlist: FieldType[K, H] :: T): List[ElementProducer] =
        (key → (hlist.head: H)) :: tf.value.write(hlist.tail)
    }
  }

  // NOTE: we use the Class[E] param here in order to allow single 
  implicit def deriveHandler[E, R <: HList](implicit gen: LabelledGeneric.Aux[E, R],
                                            f: Lazy[DerivedBSONFormats[R]]): DerivedBSONFormats[E] =
    new DerivedBSONFormats[E] {
      override def read(doc: BSONDocument): E = gen.from(f.value.read(doc))

      override def write(entity: E): List[ElementProducer] = f.value.write(gen.to(entity))
    }

}


trait GenericBsonHandlers {

  implicit def deriveHandler[T](implicit derived: Lazy[DerivedBSONFormats[T]]): BSONDocumentHandler[T] =
    BSONDocumentHandler[T](
      doc => derived.value read doc,
      entity => document(derived.value write entity: _*)
    )

}

object GenericBsonHandlers extends GenericBsonHandlers

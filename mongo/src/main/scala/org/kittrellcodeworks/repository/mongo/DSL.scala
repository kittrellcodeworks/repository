package org.kittrellcodeworks.repository
package mongo

import reactivemongo.api.bson._

/**
 * A domain-specific-language providing functional syntax for building mongo queries more simply.
 */
object DSL {

  def $and(objs: BSONDocument*): ElementProducer = "$and" -> objs

  def $or(objs: BSONDocument*): ElementProducer = "$or" -> objs

  def $gte[T](value: T)(implicit writer: BSONWriter[T]): ElementProducer = "$gte" -> value

  def $gt[T](value: T)(implicit writer: BSONWriter[T]): ElementProducer = "$gt" -> value

  def $lt[T](value: T)(implicit writer: BSONWriter[T]): ElementProducer = "$lt" -> value

  def $lte[T](value: T)(implicit writer: BSONWriter[T]): ElementProducer = "$lte" -> value

  def $in[T](values: Seq[T])(implicit writer: BSONWriter[T]): ElementProducer = "$in" -> values

  def $in[T](values: Option[Seq[T]])(implicit writer: BSONWriter[T]): ElementProducer = "$in" -> values

  implicit class StringHelper(val s: String) extends AnyVal {
    def $gte[T](value: T)(implicit writer: BSONWriter[T]): ElementProducer = s -> document(DSL.$gte(value))

    def $gt[T](value: T)(implicit writer: BSONWriter[T]): ElementProducer = s -> document(DSL.$gt(value))

    def $lt[T](value: T)(implicit writer: BSONWriter[T]): ElementProducer = s -> document(DSL.$lt(value))

    def $lte[T](value: T)(implicit writer: BSONWriter[T]): ElementProducer = s -> document(DSL.$lte(value))

    def $in[T](values: Seq[T])(implicit writer: BSONWriter[T]): ElementProducer = s -> document(DSL.$in(values))

    def $in[T](values: Option[Seq[T]])(implicit writer: BSONWriter[T]): ElementProducer = s -> document(DSL.$in(values))
  }

}

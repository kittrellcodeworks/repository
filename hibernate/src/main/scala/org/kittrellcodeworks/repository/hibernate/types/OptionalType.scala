package org.kittrellcodeworks.repository.hibernate.types

import org.hibernate.`type`.AbstractStandardBasicType
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType

import java.io
import java.sql.{PreparedStatement, ResultSet}

object OptionalType {
  val resultSetGetterParams: Array[Class[_]] = Array(classOf[String])
}

class OptionalType[T](basicType: AbstractStandardBasicType[T]) extends UserType {

  def getName: String = basicType.getName + "-option"

  override def sqlTypes(): Array[Int] = Array(basicType.getSqlTypeDescriptor.getSqlType)

  override def returnedClass(): Class[_] = classOf[Option[T]]

  override def equals(x: AnyRef, y: AnyRef): Boolean = x == y

  override def hashCode(x: AnyRef): Int =
    x.asInstanceOf[Option[T]].fold(None.hashCode)(t => basicType.getJavaTypeDescriptor.extractHashCode(t))

  override def nullSafeGet(rs: ResultSet,
                           names: Array[String],
                           session: SharedSessionContractImplementor,
                           owner: AnyRef): AnyRef =
    Option(basicType.nullSafeGet(rs, names, session, owner))

  override def nullSafeSet(st: PreparedStatement,
                           value: AnyRef,
                           index: Int,
                           session: SharedSessionContractImplementor): Unit = value match {
    case Some(t) => basicType.nullSafeSet(st, t, index, session)
    case _ => st.setNull(index, basicType.getSqlTypeDescriptor.getSqlType)
  }

  override def deepCopy(value: AnyRef): AnyRef = value

  override def isMutable: Boolean = false

  override def disassemble(value: AnyRef): io.Serializable = value.asInstanceOf[Option[T]]

  override def assemble(cached: io.Serializable, owner: AnyRef): AnyRef = cached.asInstanceOf[AnyRef]

  override def replace(original: AnyRef, target: AnyRef, owner: AnyRef): AnyRef = original

}

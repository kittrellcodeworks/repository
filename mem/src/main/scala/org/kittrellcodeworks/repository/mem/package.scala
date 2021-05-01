package org.kittrellcodeworks.repository

package object mem {

  implicit class FieldLabelOps[E](val entity: E) extends AnyVal {

    def getField(fieldName: String)(implicit f: FieldLabels[E]): Option[Any] = f.get(entity, fieldName)

  }

}

package org.kittrellcodeworks.repository.sample

import scala.beans.BeanProperty
import scala.util.matching.Regex

// Define an Entity class - This could be an Algebraic Data Type, if you want to add sub-type discrimination
case class Employee(
                     // Annotation `@BeanProperty var` required for Hibernate
                     // - mutability BREAKS functional design principles, so be very careful.
                     // - If you don't want to support Hibernate, then DO NOT use `@BeanProperty var`
                     @BeanProperty var id: Option[EmployeeId],
                     @BeanProperty var name: String,
                     @BeanProperty var manager: Option[ManagerId]
                   ) {

  // Some backends require a no-arg constructor (like Hibernate)
  def this() = this(None, "", None)
}

object Employee {

  // This code to facilitate unit testing from known large sets of data in test resources.

  val EmpStr: Regex = "([a-zA-Z0-9]+) \"([^\"]*)\"(?: ([a-zA-Z0-9]+))?".r

  def readFrom(src: scala.io.Source): Seq[Employee] =
    src.getLines().map(_.trim).flatMap {
      case null | "" => None
      case EmpStr(id, name, mgr) => Some(Employee(Some(id), name, Option(mgr).filter(_.nonEmpty)))
      case line =>
        System.err.println(s"WARN: input didn't conform to pattern: $line")
        None
    }.toSeq

}
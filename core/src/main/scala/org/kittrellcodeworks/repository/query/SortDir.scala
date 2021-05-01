package org.kittrellcodeworks.repository
package query

/**
 * The sorting direction.
 *
 * @author Sam Rushing
 */
sealed trait SortDir

/**
 * Indicates that an "ascending" sort should be performed. i.e.: natural order
 *
 * @author Sam Rushing
 */
case object Asc extends SortDir {
  override def toString: String = SortDir.ASC
}

/**
 * Indicates that an "descending" sort should be performed. i.e.: reversed order
 *
 * @author Sam Rushing
 */
case object Desc extends SortDir {
  override def toString: String = SortDir.DESC
}

object SortDir {
  private[query] val ASC = "asc"
  private[query] val DESC = "desc"

  /**
   * [[Repository]] implementations may use this extractor to convert string representations to [[SortDir]]
   */
  def unapply(name: String): Option[SortDir] = name.toLowerCase match {
    case this.ASC => Some(Asc)
    case this.DESC => Some(Desc)
    case _ => None
  }
}

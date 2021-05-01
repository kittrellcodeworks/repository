package org.kittrellcodeworks.repository
package query

/**
 * A Sortable record tells the [[Repository]] that sorting should be performed on the results of a query and the
 * field and direction of that sort.
 *
 * @param fieldName The name of the Entity's field to use for sorting
 * @param dir the direction of the sort, either [[Asc]] or [[Desc]]
 *
 * @author Sam Rushing
 */
case class Sortable(fieldName: String, dir: SortDir = Asc)

package org.kittrellcodeworks.repository
package upsert

/**
 * A description of whether an entity was inserted or updated during a [[Repository]] upsert request.
 *
 * @author Sam Rushing (rush)
 */
sealed trait UpsertAction

/**
 * Indicates that an entity was inserted during a [[Repository]] upsert request.
 *
 * @author Sam Rushing (rush)
 */
case object Inserted extends UpsertAction

/**
 * Indicates that an entity was updated during a [[Repository]] upsert request.
 *
 * @author Sam Rushing (rush)
 */
case object Updated extends UpsertAction

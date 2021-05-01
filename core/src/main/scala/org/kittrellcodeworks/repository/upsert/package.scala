package org.kittrellcodeworks.repository

package object upsert {

  type UpsertResult[Entity] = (UpsertAction, Entity)

}

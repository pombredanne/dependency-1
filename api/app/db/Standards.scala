package db

import java.util.UUID
import io.flow.postgresql.Query

/**
 * Provides docuemntation and implementation for the key attributes we
 * want on all of our findAll methods - implementing a common
 * interface to the API when searching resources.
 */
private[db] case object Standards {

  /**
    * Returns query object decorated with standard attributes in this
    * project.
    */
  def query(
    query: Query,
    tableName: String,
    auth: Clause,
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    isDeleted: Option[Boolean],
    orderBy: Option[String],
    limit: Option[Long] = Some(25),
    offset: Long = 0
  ): Query = {
    query.
      equals(s"$tableName.guid", guid).
      in(s"$tableName.guid", guids).
      condition(Some(auth.sql)).
      nullBoolean(s"$tableName.deleted_at", isDeleted).
      orderBy(orderBy).
      limit(limit).
      offset(Some(offset))
  }

}


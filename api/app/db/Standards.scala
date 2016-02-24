package db

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
    id: Option[String],
    ids: Option[Seq[String]],
    orderBy: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ): Query = {
    query.
      equals(s"$tableName.id", id).
      optionalIn(s"$tableName.id", ids).
      and(auth.sql).
      orderBy(orderBy).
      limit(limit).
      offset(offset)
  }

}


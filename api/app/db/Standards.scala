package db

import java.util.UUID

case object Standards {

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
    orderBy: OrderBy,
    limit: Option[Long] = Some(25),
    offset: Long = 0
  ): Query = {
    query.
      uuid(s"$tableName.guid", guid).
      multi(s"$tableName.guid", guids).
      condition(Some(auth.sql)).
      nullBoolean(s"$tableName.deleted_at", isDeleted).
      orderBy(orderBy.sql).
      limit(limit).
      offset(Some(offset))
  }

}


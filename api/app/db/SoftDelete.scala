package db

import anorm._
import play.api.db._
import java.util.UUID

object SoftDelete {

  private[this] val Query = """
    update %s set deleted_at=now(), updated_by_user_id = {updated_by_user_id} where guid = {guid}::uuid
  """

  def delete(tableName: String, deletedById: String, guid: UUID) {
    DB.withConnection { implicit c =>
      SQL(Query.format(tableName)).on(
        'guid -> guid,
        'updated_by_user_id = deletedById
      ).execute()
    }
  }

}

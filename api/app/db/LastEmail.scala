package db

import com.bryzek.dependency.v0.models.UserReference
import io.flow.user.v0.models.User
import io.flow.postgresql.{Query, OrderBy}
import com.bryzek.dependency.v0.models.Publication
import org.joda.time.DateTime

import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class LastEmailForm(
  userId: String,
  publication: Publication
)

case class LastEmail(
  guid: UUID,
  user: UserReference,
  publication: Publication,
  createdAt: DateTime
)

object LastEmailsDao {

  private[this] val BaseQuery = Query(s"""
    select last_emails.*
      from last_emails
  """)

  private[this] val InsertQuery = """
    insert into last_emails
    (guid, user_guid, publication, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {publication}, {updated_by_user_id})
  """

  def record(
    createdBy: User,
    form: LastEmailForm
  ): LastEmail = {
    val guid = DB.withTransaction { implicit c =>
      findByUserGuidAndPublication(form.userGuid, form.publication).foreach { rec =>
        SoftDelete.delete(c, "last_emails", createdBy.id, rec.guid)
      }
      create(createdBy, form)
    }
    findByGuid(guid).getOrElse {
      sys.error("Failed to record last email")
    }
  }

  def softDelete(deletedBy: User, rec: LastEmail) {
    SoftDelete.delete("last_emails", deletedBy.id, rec.guid)
  }

  private[this] def create(
    createdBy: User,
    form: LastEmailForm
  ) (
    implicit c: java.sql.Connection
  ): UUID = {
    val guid = UUID.randomUUID
    SQL(InsertQuery).on(
      'guid -> guid,
      'user_guid -> form.userGuid,
      'publication -> form.publication.toString,
      'updated_by_user_id -> createdBy.id
    ).execute()
    guid
  }

  def findByGuid(guid: UUID): Option[LastEmail] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findByUserGuidAndPublication(userId: String, publication: Publication): Option[LastEmail] = {
    findAll(userGuid = Some(userGuid), publication = Some(publication), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userId: Option[String] = None,
    publication: Option[Publication] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("-last_emails.publication, last_emails.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[LastEmail] = {

    DB.withConnection { implicit c =>
      BaseQuery.
        equals("last_emails.guid", guid).
        in("last_emails.guid", guids).
        equals("last_emails.user_guid", userId).
        text("last_emails.publication", publication).
        nullBoolean("last_emails.deleted_at", isDeleted).
        orderBy(orderBy.sql).
        limit(Some(limit)).
        offset(Some(offset)).
        as(parser.*)
    }
  }

  private[this] val parser: RowParser[LastEmail] = {
    SqlParser.get[_root_.java.util.UUID]("guid") ~
    SqlParser.str("user_id") ~
    com.bryzek.dependency.v0.anorm.parsers.Publication.parser(
      com.bryzek.dependency.v0.anorm.parsers.Publication.Mappings("publication")
    ) ~
    SqlParser.get[DateTime]("created_at") map {
      case guid ~ user ~ publication ~ createdAt => {
        LastEmail(
          guid = guid,
          user = user,
          publication = publication,
          createdAt = createdAt
        )
      }
    }
  }


}

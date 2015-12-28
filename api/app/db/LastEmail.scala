package db

import io.flow.common.v0.models.{Audit, Reference}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Query, OrderBy, SoftDelete}
import com.bryzek.dependency.v0.models.Publication

import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class LastEmailForm(
  userGuid: UUID,
  publication: Publication
)

case class LastEmail(
  guid: UUID,
  user: Reference,
  publication: Publication,
  audit: Audit
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
    ({guid}::equals, {user_guid}::equals, {publication}, {created_by_guid}::equals, {created_by_guid}::equals)
  """

  def record(
    createdBy: User,
    form: LastEmailForm
  ): LastEmail = {
    val guid = DB.withTransaction { implicit c =>
      findByUserGuidAndPublication(form.userGuid, form.publication).foreach { rec =>
        SoftDelete.delete(c, "last_emails", createdBy.guid, rec.guid)
      }
      create(createdBy, form)
    }
    findByGuid(guid).getOrElse {
      sys.error("Failed to record last email")
    }
  }

  def softDelete(deletedBy: User, rec: LastEmail) {
    SoftDelete.delete("last_emails", deletedBy.guid, rec.guid)
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
      'created_by_guid -> createdBy.guid
    ).execute()
    guid
  }

  def findByGuid(guid: UUID): Option[LastEmail] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findByUserGuidAndPublication(userGuid: UUID, publication: Publication): Option[LastEmail] = {
    findAll(userGuid = Some(userGuid), publication = Some(publication), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
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
        equals("last_emails.user_guid", userGuid).
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
    io.flow.common.v0.anorm.parsers.Reference.parser(
      io.flow.common.v0.anorm.parsers.Reference.Mappings(
        guid = "user_guid"
      )
    ) ~
    com.bryzek.dependency.v0.anorm.parsers.Publication.parser(
      com.bryzek.dependency.v0.anorm.parsers.Publication.Mappings("publication")
    ) ~
    io.flow.common.v0.anorm.parsers.Audit.parser(
      io.flow.common.v0.anorm.parsers.Audit.Mappings.base
    ) map {
      case guid ~ user ~ publication ~ audit => {
        LastEmail(
          guid = guid,
          user = user,
          publication = publication,
          audit = audit
        )
      }
    }
  }


}

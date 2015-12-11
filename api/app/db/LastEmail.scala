package db

import io.flow.common.v0.models.{Audit, Reference}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
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

  private[this] val BaseQuery = s"""
    select last_emails.*
      from last_emails
     where true
  """

  private[this] val InsertQuery = """
    insert into last_emails
    (guid, user_guid, publication, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {publication}, {created_by_guid}::uuid, {created_by_guid}::uuid)
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
    limit: Long = 25,
    offset: Long = 0
  ): Seq[LastEmail] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v =>  "and last_emails.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("last_emails.guid", _) },
      userGuid.map { v => "and last_emails.user_guid = {user_guid}::uuid" },
      publication.map { v => "and last_emails.publication = {publication}" },
      isDeleted.map(Filters.isDeleted("last_emails", _)),
      Some(s"order by last_emails.publication, last_emails.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      publication.map('publication -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        parser.*
      )
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

package db

import com.bryzek.dependency.v0.models.{Organization, OrganizationForm}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object OrganizationsDao {

  private[this] val BaseQuery = s"""
    select organizations.guid,
           organizations.key,
           ${AuditsDao.all("organizations")}
      from organizations
     where true
  """

  private[this] val InsertQuery = """
    insert into organizations
    (guid, key, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {key}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val UpdateQuery = """
    update organizations
       set key = {key},
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  private[db] def validate(
    form: OrganizationForm,
    existing: Option[Organization] = None
  ): Seq[String] = {
    if (form.key.trim == "") {
      Seq("Key cannot be empty")

    } else {
      OrganizationsDao.findByKey(form.key) match {
        case None => Seq.empty
        case Some(p) => {
          Some(p.guid) == existing.map(_.guid) match {
            case true => Nil
            case false => Seq("Organization with this key already exists")
          }
        }
      }
    }
  }

  def create(createdBy: User, form: OrganizationForm): Either[Seq[String], Organization] = {
    validate(form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'key -> form.key.trim,
            'created_by_guid -> createdBy.guid
          ).execute()
        }

        Right(
          findByGuid(guid).getOrElse {
            sys.error("Failed to create organization")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def update(createdBy: User, organization: Organization, form: OrganizationForm): Either[Seq[String], Organization] = {
    validate(form, Some(organization)) match {
      case Nil => {
        DB.withConnection { implicit c =>
          SQL(UpdateQuery).on(
            'guid -> organization.guid,
            'key -> form.key.trim,
            'updated_by_guid -> createdBy.guid
          ).execute()
        }

        Right(
          findByGuid(organization.guid).getOrElse {
            sys.error("Failed to create organization")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, organization: Organization) {
    SoftDelete.delete("organizations", deletedBy.guid, organization.guid)
  }

  def findByKey(key: String): Option[Organization] = {
    findAll(key = Some(key), limit = 1).headOption
  }

  def findByGuid(guid: UUID): Option[Organization] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    key: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Organization] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and organizations.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("organizations.guid", _) },
      key.map { v => "and lower(organizations.key) = lower(trim({key}))" },
      isDeleted.map(Filters.isDeleted("organizations", _)),
      Some(s"order by organizations.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      key.map('key -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Organization.table("organizations").*
      )
    }
  }

}

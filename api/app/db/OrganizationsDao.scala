package db

import com.bryzek.dependency.v0.models.{MembershipForm, Organization, OrganizationForm, Role}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.play.util.UrlKey
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object OrganizationsDao {

  val DefaultUserNameLength = 8

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

  private[this] val InsertUserOrganizationQuery = """
    insert into user_organizations
    (guid, user_guid, organization_guid, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {organization_guid}::uuid, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val urlKey = UrlKey(minKeyLength = 3)

  private[db] def validate(
    form: OrganizationForm,
    existing: Option[Organization] = None
  ): Seq[String] = {
    if (form.key.trim == "") {
      Seq("Key cannot be empty")

    } else {
      OrganizationsDao.findByKey(Authorization.All, form.key) match {
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
        val guid = DB.withTransaction { implicit c =>
          create(c, createdBy, form)
        }
        Right(
          findByGuid(Authorization.All, guid).getOrElse {
            sys.error("Failed to create organization")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  private[this] def create(implicit c: java.sql.Connection, createdBy: User, form: OrganizationForm): UUID = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'key -> form.key.trim,
      'created_by_guid -> createdBy.guid
    ).execute()

    MembershipsDao.create(
      c,
      createdBy,
      MembershipForm(
        userGuid = createdBy.guid,
        organizationGuid = guid,
        role = Role.Admin
      )
    )
    
    guid
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
          findByGuid(Authorization.All, organization.guid).getOrElse {
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

  def upsertForUser(user: User): Organization = {
    findAll(Authorization.All, forUserGuid = Some(user.guid), limit = 1).headOption.getOrElse {
      val key = urlKey.generate(defaultUserName(user))
      val orgGuid = DB.withTransaction { implicit c =>
        val orgGuid = create(c, user, OrganizationForm(
          key = key
        ))

        SQL(InsertUserOrganizationQuery).on(
          'guid -> UUID.randomUUID,
          'user_guid -> user.guid,
          'organization_guid -> orgGuid,
          'created_by_guid -> user.guid
        ).execute()

        orgGuid
      }
      findByGuid(Authorization.All, orgGuid).getOrElse {
        sys.error(s"Failed to create an organization for the user[$user]")
      }
    }
  }

  /**
   * Generates a default username for this user based on email or
   * name.
   */
  def defaultUserName(user: User): String = {
    urlKey.format(
      user.email match {
        case Some(email) => {
          email.substring(0, email.indexOf("@"))
        }
        case None => {
          (user.name.first, user.name.last) match {
            case (None, None) => urlKey.randomAlphanumericString(DefaultUserNameLength)
            case (Some(first), None) => first
            case (None, Some(last)) => last
            case (Some(first), Some(last)) => first(0) + last
          }
        }
      }
    )
  }

  def findByKey(auth: Authorization, key: String): Option[Organization] = {
    findAll(auth, key = Some(key), limit = 1).headOption
  }

  def findByGuid(auth: Authorization, guid: UUID): Option[Organization] = {
    findAll(auth, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
    key: Option[String] = None,
    forUserGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Organization] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      Some(auth.organizations("organizations.guid").and),
      guid.map { v => "and organizations.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("organizations.guid", _) },
      userGuid.map { v => "and organizations.guid in (select organization_guid from memberships where deleted_at is null and user_guid = {user_guid}::uuid)" },
      key.map { v => "and lower(organizations.key) = lower(trim({key}))" },
      forUserGuid.map { v => "and organizations.guid = (select organization_guid from user_organizations where deleted_at is null and user_guid = {for_user_guid}::uuid)" },
      isDeleted.map(Filters.isDeleted("organizations", _)),
      Some(s"order by organizations.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      key.map('key -> _.toString),
      userGuid.map('user_guid -> _.toString),
      forUserGuid.map('for_user_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Organization.table("organizations").*
      )
    }
  }

}

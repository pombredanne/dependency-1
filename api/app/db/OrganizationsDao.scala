package db

import com.bryzek.dependency.v0.models.{MembershipForm, Organization, OrganizationForm, Role}
import io.flow.postgresql.{Query, OrderBy}
import io.flow.play.util.{Random, UrlKey}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object OrganizationsDao {

  val DefaultUserNameLength = 8

  private[this] val BaseQuery = Query(s"""
    select organizations.guid,
           organizations.key
      from organizations
  """)

  private[this] val InsertQuery = """
    insert into organizations
    (guid, key, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {key}, {updated_by_user_id})
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
    ({guid}::uuid, {user_guid}::uuid, {organization_guid}::uuid, {updated_by_user_id})
  """

  private[this] val random = Random()
  private[this] val urlKey = UrlKey(minKeyLength = 3)

  private[db] def validate(
    form: OrganizationForm,
    existing: Option[Organization] = None
  ): Seq[String] = {
    if (form.key.trim == "") {
      Seq("Key cannot be empty")

    } else {
      urlKey.validate(form.key.trim) match {
        case Nil => {
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
        case errors => errors
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
      'updated_by_user_id -> createdBy.id
    ).execute()

    MembershipsDao.create(
      c,
      createdBy,
      guid,
      createdBy.id,
      Role.Admin
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
            'updated_by_guid -> createdBy.id
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
    SoftDelete.delete("organizations", deletedBy.id, organization.guid)
  }

  def upsertForUser(user: User): Organization = {
    findAll(Authorization.All, forUserGuid = Some(user.id), limit = 1).headOption.getOrElse {
      val key = urlKey.generate(defaultUserName(user))
      val orgGuid = DB.withTransaction { implicit c =>
        val orgGuid = create(c, user, OrganizationForm(
          key = key
        ))

        SQL(InsertUserOrganizationQuery).on(
          'guid -> UUID.randomUUID,
          'user_guid -> user.id,
          'organization_guid -> orgGuid,
          'created_by_guid -> user.id
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
            case (None, None) => random.alphanumeric(DefaultUserNameLength)
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
    userId: Option[String] = None,
    key: Option[String] = None,
    forUserGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("organizations.key, -organizations.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Organization] = {
    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "organizations",
        auth = auth.organizations("organizations.guid"),
        guid = guid,
        guids = guids,
        orderBy = orderBy.sql,
        isDeleted = isDeleted,
        limit = Some(limit),
        offset = offset
      ).
        subquery("organizations.guid", "user_guid", userGuid, { bindVar =>
          s"select organization_guid from memberships where deleted_at is null and user_guid = ${bindVar.sql}"
        }).
        text(
          "organizations.key",
          key,
          columnFunctions = Seq(Query.Function.Lower),
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        subquery("organizations.guid", "for_user_guid", forUserGuid, { bindVar =>
          s"select organization_guid from user_organizations where deleted_at is null and user_guid = ${bindVar.sql}"
        }).
        as(
          com.bryzek.dependency.v0.anorm.parsers.Organization.table("organizations").*
        )
    }
  }

}

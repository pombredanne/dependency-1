package db

import com.bryzek.dependency.v0.models.{Membership, MembershipForm, Organization, OrganizationSummary, Role}
import io.flow.play.postgresql.{AuditsDao, Query, OrderBy, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object MembershipsDao {

  val DefaultUserNameLength = 8

  private[this] val BaseQuery = Query(s"""
    select memberships.guid,
           memberships.role,
           ${AuditsDao.all("memberships")},
           organizations.guid as memberships_organization_guid,
           organizations.key as memberships_organization_key,
           users.guid as memberships_user_guid,
           users.email  as memberships_user_email,
           users.first_name as memberships_user_name_first,
           users.last_name as memberships_user_name_last
      from memberships
      join organizations on organizations.deleted_at is null and organizations.guid = memberships.organization_guid
      join users on users.deleted_at is null and users.guid = memberships.user_guid
  """)

  private[this] val InsertQuery = """
    insert into memberships
    (guid, role, user_guid, organization_guid, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {role}, {user_guid}::uuid, {organization_guid}::uuid, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def isMember(orgGuid: UUID, user: User): Boolean = {
    MembershipsDao.findByOrganizationGuidAndUserGuid(Authorization.All, orgGuid, user.guid) match {
      case None => false
      case Some(_) => true
    }
  }

  private[db] def validate(
    user: User,
    form: MembershipForm
  ): Seq[String] = {
    val roleErrors = form.role match {
      case Role.UNDEFINED(_) => Seq("Invalid role. Must be one of: " + Role.all.map(_.toString).mkString(", "))
      case _ => {
        MembershipsDao.findByOrganizationGuidAndUserGuid(Authorization.All, form.organizationGuid, form.userGuid) match {
          case None => Seq.empty
          case Some(membership) => {
            if (membership.role == form.role) {
              Seq("Membership already exists")
            } else {
              Nil
            }
          }
        }
      }
    }

    val organizationErrors = MembershipsDao.isMember(form.organizationGuid, user) match  {
      case false => Seq("Organization does not exist or you are not authorized to access this organization")
      case true => Nil
    }

    roleErrors ++ organizationErrors
  }

  def create(createdBy: User, form: MembershipForm): Either[Seq[String], Membership] = {
    validate(createdBy, form) match {
      case Nil => {
        val guid = MembershipsDao.findByOrganizationGuidAndUserGuid(Authorization.All, form.organizationGuid, form.userGuid) match {
          case None => {
            DB.withConnection { implicit c =>
              create(c, createdBy, form)
            }
          }
          case Some(existing) => {
            // the role is changing. Replace record
            DB.withTransaction { implicit c =>
              SoftDelete.delete(c, "memberships", createdBy.guid, existing.guid)
              create(c, createdBy, form)
            }
          }
        }
        Right(
          findByGuid(Authorization.All, guid).getOrElse {
            sys.error("Failed to create membership")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: User, form: MembershipForm): UUID = {
    val guid = UUID.randomUUID
    SQL(InsertQuery).on(
      'guid -> guid,
      'user_guid -> form.userGuid,
      'organization_guid -> form.organizationGuid,
      'role -> form.role.toString,
      'created_by_guid -> createdBy.guid
    ).execute()
    guid
  }

  def softDelete(deletedBy: User, membership: Membership) {
    SoftDelete.delete("memberships", deletedBy.guid, membership.guid)
  }

  def findByOrganizationGuidAndUserGuid(
    auth: Authorization,
    organizationGuid: UUID,
    userGuid: UUID
  ): Option[Membership] = {
    findAll(
      auth,
      organizationGuid = Some(organizationGuid),
      userGuid = Some(userGuid),
      limit = 1
    ).headOption
  }

  def findByGuid(auth: Authorization, guid: UUID): Option[Membership] = {
    findAll(auth, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    organizationGuid: Option[UUID] = None,
    userGuid: Option[UUID] = None,
    role: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy.parseOrError("memberships.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Membership] = {
    DB.withConnection { implicit c =>
    Standards.query(
      BaseQuery,
      tableName = "memberships",
      auth = auth.organizations("organizations.guid"),
      guid = guid,
      guids = guids,
      orderBy = orderBy.sql,
      isDeleted = isDeleted,
      limit = Some(limit),
      offset = offset
    ).
      uuid("memberships.organization_guid", organizationGuid).
      uuid("memberships.user_guid", userGuid).
      text(
        "memberships.role",
        role,
        valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
      ).
      as(
        com.bryzek.dependency.v0.anorm.parsers.Membership.table("memberships").*
      )
    }
  }

}

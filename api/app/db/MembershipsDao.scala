package db

import com.bryzek.dependency.v0.models.{Membership, MembershipForm, Organization, OrganizationSummary, Role}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object MembershipsDao {

  val DefaultUserNameLength = 8

  private[this] val BaseQuery = s"""
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
     where true
  """

  private[this] val InsertQuery = """
    insert into memberships
    (guid, role, user_guid, organization_guid, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {role}, {user_guid}::uuid, {organization_guid}::uuid, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def isMember(orgGuid: UUID, user: User): Boolean = {
    MembershipsDao.findByOrganizationGuidAndUserGuid(orgGuid, user.guid) match {
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
        MembershipsDao.findByOrganizationGuidAndUserGuid(form.organizationGuid, form.userGuid) match {
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
        val guid = MembershipsDao.findByOrganizationGuidAndUserGuid(form.organizationGuid, form.userGuid) match {
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
          findByGuid(guid).getOrElse {
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
    organizationGuid: UUID,
    userGuid: UUID
  ): Option[Membership] = {
    findAll(
      organizationGuid = Some(organizationGuid),
      userGuid = Some(userGuid),
      limit = 1
    ).headOption
  }

  def findByGuid(guid: UUID): Option[Membership] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    organizationGuid: Option[UUID] = None,
    userGuid: Option[UUID] = None,
    role: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Membership] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and memberships.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("memberships.guid", _) },
      organizationGuid.map { v => "and memberships.organization_guid = {organization_guid}::uuid" },
      userGuid.map { v => "and memberships.user_guid = {user_guid}::uuid" },
      role.map { v => "and memberships.role = lower(trim({role}))" },
      isDeleted.map(Filters.isDeleted("memberships", _)),
      Some(s"order by memberships.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      role.map('role -> _.toString),
      userGuid.map('user_guid -> _.toString),
      organizationGuid.map('organization_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Membership.table("memberships").*
      )
    }
  }

}

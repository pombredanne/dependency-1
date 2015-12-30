package db

import com.bryzek.dependency.v0.models.{Membership, MembershipForm, Organization, OrganizationSummary, Role}
import io.flow.postgresql.{Query, OrderBy}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

object MembershipsDao {

  val DefaultUserNameLength = 8

  private[this] val BaseQuery = Query(s"""
    select memberships.id,
           memberships.role,
           organizations.id as memberships_organization_id,
           organizations.key as memberships_organization_key,
           users.id as memberships_user_id,
           users.email  as memberships_user_email,
           users.first_name as memberships_user_name_first,
           users.last_name as memberships_user_name_last
      from memberships
      join organizations on organizations.deleted_at is null and organizations.id = memberships.organization_id
      join users on users.deleted_at is null and users.id = memberships.user_id
  """)

  private[this] val InsertQuery = """
    insert into memberships
    (id, role, user_id, organization_id, updated_by_user_id)
    values
    ({id}, {role}, {user_id}, {organization_id}, {updated_by_user_id})
  """

  def isMemberByOrgId(orgId: String, user: User): Boolean = {
    MembershipsDao.findByOrganizationIdAndUserId(Authorization.All, orgId, user.id) match {
      case None => false
      case Some(_) => true
    }
  }

  def isMemberByOrgKey(org: String, user: User): Boolean = {
    MembershipsDao.findByOrganizationAndUserId(Authorization.All, org, user.id) match {
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
        MembershipsDao.findByOrganizationAndUserId(Authorization.All, form.organization, form.userId) match {
          case None => Seq.empty
          case Some(membership) => {
            Seq("User is already a member")
          }
        }
      }
    }

    val organizationErrors = MembershipsDao.findByOrganizationAndUserId(Authorization.All, form.organization, user.id) match {
      case None => Seq("Organization does not exist or you are not authorized to access this organization")
      case Some(_) => Nil
    }

    roleErrors ++ organizationErrors
  }

  def create(createdBy: User, form: MembershipForm): Either[Seq[String], Membership] = {
    validate(createdBy, form) match {
      case Nil => {
        val id = MembershipsDao.findByOrganizationAndUserId(Authorization.All, form.organization, form.userId) match {
          case None => {
            DB.withConnection { implicit c =>
              create(c, createdBy, form)
            }
          }
          case Some(existing) => {
            // the role is changing. Replace record
            DB.withTransaction { implicit c =>
              SoftDelete.delete(c, "memberships", createdBy.id, existing.id)
              create(c, createdBy, form)
            }
          }
        }
        Right(
          findById(Authorization.All, id).getOrElse {
            sys.error("Failed to create membership")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: User, form: MembershipForm): String = {
    val org = OrganizationsDao.findByKey(Authorization.All, form.organization).getOrElse {
      sys.error("Could not find organization with key[${form.organization}]")
    }

    create(c, createdBy, org.id, form.userId, form.role)
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: User, orgId: String, userId: String, role: Role): String = {
    val id = io.flow.play.util.IdGenerator("mem").randomId()

    SQL(InsertQuery).on(
      'id -> id,
      'user_id -> userId,
      'organization_id -> orgId,
      'role -> role.toString,
      'updated_by_user_id -> createdBy.id
    ).execute()
    id
  }

  def softDelete(deletedBy: User, membership: Membership) {
    SoftDelete.delete("memberships", deletedBy.id, membership.id)
  }

  def findByOrganizationAndUserId(
    auth: Authorization,
    organization: String,
    userId: String
  ): Option[Membership] = {
    findAll(
      auth,
      organization = Some(organization),
      userId = Some(userId),
      limit = 1
    ).headOption
  }

  def findByOrganizationIdAndUserId(
    auth: Authorization,
    organizationId: String,
    userId: String
  ): Option[Membership] = {
    findAll(
      auth,
      organizationId = Some(organizationId),
      userId = Some(userId),
      limit = 1
    ).headOption
  }

  def findById(auth: Authorization, id: String): Option[Membership] = {
    findAll(auth, id = Some(id), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
     organization: Option[String] = None,
    organizationId: Option[String] = None,
    userId: Option[String] = None,
    role: Option[Role] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("memberships.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Membership] = {
    DB.withConnection { implicit c =>
    Standards.query(
      BaseQuery,
      tableName = "memberships",
      auth = auth.organizations("organizations.id"),
      id = id,
      ids = ids,
      orderBy = orderBy.sql,
      isDeleted = isDeleted,
      limit = Some(limit),
      offset = offset
    ).
      equals("memberships.organization_id", organizationId).
      text("organizations.key", organization, valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)).
      equals("memberships.user_id", userId).
      text("memberships.role", role.map(_.toString.toLowerCase)).
      as(
        com.bryzek.dependency.v0.anorm.parsers.Membership.table("memberships").*
      )
    }
  }

}

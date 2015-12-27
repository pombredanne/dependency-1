package db

import com.bryzek.dependency.v0.models.{Membership, OrganizationSummary, Role}
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class MembershipsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()
  lazy val user = createUser()
  lazy val membership = createMembership(createMembershipForm(org = org, user = user))

  "isMember by guid" in {
    membership // Create the membership record

    MembershipsDao.isMember(org.guid, user) must be(true)
    MembershipsDao.isMember(org.guid, createUser()) must be(false)
    MembershipsDao.isMember(createOrganization().guid, user) must be(false)
  }

  "isMember by key" in {
    membership // Create the membership record

    MembershipsDao.isMember(org.key, user) must be(true)
    MembershipsDao.isMember(org.key, createUser()) must be(false)
    MembershipsDao.isMember(createOrganization().key, user) must be(false)
  }

  "findByOrganizationGuidAndUserGuid" in {
    membership // Create the membership record

    MembershipsDao.findByOrganizationGuidAndUserGuid(Authorization.All, org.guid, user.guid).map(_.guid) must be(
      Some(membership.guid)
    )

    MembershipsDao.findByOrganizationGuidAndUserGuid(Authorization.All, UUID.randomUUID, user.guid) must be(None)
    MembershipsDao.findByOrganizationGuidAndUserGuid(Authorization.All, org.guid, UUID.randomUUID) must be(None)
  }

  "findByGuid" in {
    MembershipsDao.findByGuid(Authorization.All, membership.guid).map(_.guid) must be(
      Some(membership.guid)
    )

    MembershipsDao.findByGuid(Authorization.All, UUID.randomUUID) must be(None)
  }

  "soft delete" in {
    val membership = createMembership()
    MembershipsDao.softDelete(systemUser, membership)
    MembershipsDao.findByGuid(Authorization.All, membership.guid) must be(None)
  }

  "validates role" in {
    val form = createMembershipForm(role = Role.UNDEFINED("other"))
    MembershipsDao.validate(systemUser, form) must be(Seq("Invalid role. Must be one of: member, admin"))
  }

  "validates duplicate" in {
    val org = createOrganization()
    val user = createUser()
    val form = createMembershipForm(org = org, user = user, role = Role.Member)
    val membership = createMembership(form)

    MembershipsDao.validate(systemUser, form) must be(Seq("User is already a member"))
    MembershipsDao.validate(systemUser, form.copy(role = Role.Admin)) must be(Seq("User is already a member"))
  }

  "validates access to org" in {
    MembershipsDao.validate(createUser(), createMembershipForm()) must be(
      Seq("Organization does not exist or you are not authorized to access this organization")
    )
  }

  "findAll" must {

    "guids" in {
      val membership2 = createMembership()

      MembershipsDao.findAll(Authorization.All, guids = Some(Seq(membership.guid, membership2.guid))).map(_.guid) must be(
        Seq(membership.guid, membership2.guid)
      )

      MembershipsDao.findAll(Authorization.All, guids = Some(Nil)) must be(Nil)
      MembershipsDao.findAll(Authorization.All, guids = Some(Seq(UUID.randomUUID))) must be(Nil)
      MembershipsDao.findAll(Authorization.All, guids = Some(Seq(membership.guid, UUID.randomUUID))).map(_.guid) must be(Seq(membership.guid))
    }

    "userGuid" in {
      MembershipsDao.findAll(Authorization.All, guid = Some(membership.guid), userGuid = Some(user.guid)).map(_.guid) must be(
        Seq(membership.guid)
      )

      MembershipsDao.findAll(Authorization.All, guid = Some(membership.guid), userGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "organizationGuid" in {
      MembershipsDao.findAll(Authorization.All, guid = Some(membership.guid), organizationGuid = Some(membership.organization.guid)).map(_.guid) must be(
        Seq(membership.guid)
      )

      MembershipsDao.findAll(Authorization.All, guid = Some(membership.guid), organizationGuid = Some(UUID.randomUUID)) must be(Nil)
    }
  }
}

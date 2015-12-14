package db

import com.bryzek.dependency.v0.models.{Membership, Role}
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

  "findByOrganizationGuidAndUserGuid" in {
    membership // Create the membership record

    MembershipsDao.findByOrganizationGuidAndUserGuid(org.guid, user.guid).map(_.guid) must be(
      Some(membership.guid)
    )

    MembershipsDao.findByOrganizationGuidAndUserGuid(UUID.randomUUID, user.guid) must be(None)
    MembershipsDao.findByOrganizationGuidAndUserGuid(org.guid, UUID.randomUUID) must be(None)
  }

  "findByGuid" in {
    MembershipsDao.findByGuid(membership.guid).map(_.guid) must be(
      Some(membership.guid)
    )

    MembershipsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "changes role" in {
    val org = createOrganization()
    val user = createUser()
    val form = createMembershipForm(org = org, user = user, role = Role.Member)

    createMembership(form.copy(role = Role.Member))
    MembershipsDao.findByOrganizationGuidAndUserGuid(org.guid, user.guid).get.role must be(Role.Member)

    createMembership(form.copy(role = Role.Admin))
    MembershipsDao.findByOrganizationGuidAndUserGuid(org.guid, user.guid).get.role must be(Role.Admin)

    createMembership(form.copy(role = Role.Member))
    MembershipsDao.findByOrganizationGuidAndUserGuid(org.guid, user.guid).get.role must be(Role.Member)
  }

  "soft delete" in {
    val membership = createMembership()
    MembershipsDao.softDelete(systemUser, membership)
    MembershipsDao.findByGuid(membership.guid) must be(None)
  }

  "validates role" in {
    val form = createMembershipForm(role = Role.UNDEFINED("other"))
    MembershipsDao.validate(form) must be(Seq("Invalid role. Must be one of: member, admin"))
  }

  "validates duplicate" in {
    val org = createOrganization()
    val user = createUser()
    val form = createMembershipForm(org = org, user = user, role = Role.Member)
    val membership = createMembership(form)

    MembershipsDao.validate(form) must be(Seq("Membership already exists"))
    MembershipsDao.validate(form.copy(role = Role.Admin)) must be(Nil)
  }

  "findAll" must {

    "guids" in {
      val membership2 = createMembership()

      MembershipsDao.findAll(guids = Some(Seq(membership.guid, membership2.guid))).map(_.guid) must be(
        Seq(membership.guid, membership2.guid)
      )

      MembershipsDao.findAll(guids = Some(Nil)) must be(Nil)
      MembershipsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
      MembershipsDao.findAll(guids = Some(Seq(membership.guid, UUID.randomUUID))).map(_.guid) must be(Seq(membership.guid))
    }

    "userGuid" in {
      MembershipsDao.findAll(guid = Some(membership.guid), userGuid = Some(user.guid)).map(_.guid) must be(
        Seq(membership.guid)
      )

      MembershipsDao.findAll(guid = Some(membership.guid), userGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "organizationGuid" in {
      MembershipsDao.findAll(guid = Some(membership.guid), organizationGuid = Some(membership.organization.guid)).map(_.guid) must be(
        Seq(membership.guid)
      )

      MembershipsDao.findAll(guid = Some(membership.guid), organizationGuid = Some(UUID.randomUUID)) must be(Nil)
    }
  }
}

package db

import io.flow.user.v0.models.Name
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class OrganizationsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "defaultUserName" in {
    val user = makeUser()

    OrganizationsDao.defaultUserName(
      user.copy(email = Some("mike@flow.io"))
    ) must be("mike")

    OrganizationsDao.defaultUserName(
      user.copy(email = Some("mbryzek@alum.mit.edu"))
    ) must be("mbryzek")

    OrganizationsDao.defaultUserName(
      user.copy(name = Name())
    ).length must be(OrganizationsDao.DefaultUserNameLength)

    OrganizationsDao.defaultUserName(
      user.copy(name = Name(first = Some("Michael")))
    ) must be("michael")

    OrganizationsDao.defaultUserName(
      user.copy(name = Name(last = Some("Bryzek")))
    ) must be("bryzek")

    OrganizationsDao.defaultUserName(
      user.copy(name = Name(first = Some("Michael"), last = Some("Bryzek")))
    ) must be("mbryzek")
  }

  "create" in {
    val form = createOrganizationForm()
    val organization = OrganizationsDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create org")
    }
    organization.key must be(form.key)
  }

  "softDelete" in {
    val org = createOrganization()
    OrganizationsDao.softDelete(systemUser, org)
    OrganizationsDao.findByGuid(org.guid) must be(None)
    OrganizationsDao.findAll(guid = Some(org.guid), isDeleted = Some(false)) must be(Nil)
    OrganizationsDao.findAll(guid = Some(org.guid), isDeleted = Some(true)).map(_.guid) must be(Seq(org.guid))
  }

  "findByGuid" in {
    val organization = createOrganization()
    OrganizationsDao.findByGuid(organization.guid).map(_.guid) must be(
      Some(organization.guid)
    )

    OrganizationsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val organization1 = createOrganization()
    val organization2 = createOrganization()

    OrganizationsDao.findAll(guids = Some(Seq(organization1.guid, organization2.guid))).map(_.guid).sorted must be(
      Seq(organization1.guid, organization2.guid).sorted
    )

    OrganizationsDao.findAll(guids = Some(Nil)) must be(Nil)
    OrganizationsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    OrganizationsDao.findAll(guids = Some(Seq(organization1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(organization1.guid))
  }

  "findAll by userGuid includes user's org" in {
    val user = createUser()

    waitFor { () =>
      !OrganizationsDao.findAll(forUserGuid = Some(user.guid)).isEmpty
    }

    val org = OrganizationsDao.findAll(forUserGuid = Some(user.guid)).head
    OrganizationsDao.findAll(guid = Some(org.guid), forUserGuid = Some(user.guid)).map(_.guid) must be(Seq(org.guid))
    OrganizationsDao.findAll(guid = Some(org.guid), forUserGuid = Some(UUID.randomUUID)) must be(Nil)
  }

}

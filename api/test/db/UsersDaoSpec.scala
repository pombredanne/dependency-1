package db

import io.flow.user.v0.models.{Name, NameForm, UserForm}
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class UsersDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "Special users" must {
    "anonymous user exists" in {
      UsersDao.anonymousUser.email must be(
        Some(UsersDao.AnonymousEmailAddress)
      )
    }

    "system user exists" in {
      UsersDao.systemUser.email must be(
        Some(UsersDao.SystemEmailAddress)
      )
    }

    "system and anonymous users are different" in {
      UsersDao.AnonymousEmailAddress must not be(
        UsersDao.SystemEmailAddress
      )

      UsersDao.anonymousUser.guid must not be(
        UsersDao.systemUser.guid
      )
    }

  }

  "findByEmail" in {
    UsersDao.findByEmail(UsersDao.SystemEmailAddress).flatMap(_.email) must be(
      Some(UsersDao.SystemEmailAddress)
    )

    UsersDao.findByEmail(UUID.randomUUID.toString) must be(None)
  }

  "findByGuid" in {
    UsersDao.findByGuid(UsersDao.systemUser.guid).map(_.guid) must be(
      Some(UsersDao.systemUser.guid)
    )

    UsersDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findByGuid" in {
    val user = createUser()
    val githubUser = createGithubUser(createGithubUserForm(user = user))

    UsersDao.findByGithubUserId(githubUser.id).map(_.guid) must be(
      Some(user.guid)
    )

    UsersDao.findByGithubUserId(0) must be(None)
  }


  "findAll" must {

    "filter by guids" in {
      val user1 = createUser()
      val user2 = createUser()

      UsersDao.findAll(guids = Some(Seq(user1.guid, user2.guid))).map(_.guid) must be(
        Seq(user1.guid, user2.guid)
      )

      UsersDao.findAll(guids = Some(Nil)) must be(Nil)
      UsersDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
      UsersDao.findAll(guids = Some(Seq(user1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(user1.guid))
    }

    "filter by email" in {
      val user = createUser()
      val email = user.email.getOrElse {
        sys.error("user must have email address")
      }

      UsersDao.findAll(guid = Some(user.guid), email = Some(email)).map(_.guid) must be(Seq(user.guid))
      UsersDao.findAll(guid = Some(user.guid), email = Some(createTestEmail())) must be(Nil)
    }

    "filter by identifier" in {
      val user = createUser()
      val identifier = UserIdentifiersDao.latestForUser(systemUser, user).value

      UsersDao.findAll(identifier = Some(identifier)).map(_.guid) must be(Seq(user.guid))
      UsersDao.findAll(identifier = Some(createTestKey())) must be(Nil)
    }

  }

  "create" must {
    "user with email and name" in {
      val email = createTestEmail()
      val name = NameForm(
        first = Some("Michael"),
        last = Some("Bryzek")
      )
      UsersDao.create(
        createdBy = None,
        form = createUserForm(
          email = email,
          name = Some(name)
        )
      ) match {
        case Left(errors) => fail(errors.mkString(", "))
        case Right(user) => {
          user.email must be(Some(email))
          user.name.first must be(name.first)
          user.name.last must be(name.last)
        }
      }
    }

    "processes empty name" in {
      val name = NameForm(
        first = Some("  "),
        last = Some("   ")
      )
      UsersDao.create(
        createdBy = None,
        form = createUserForm().copy(name = Some(name))
      ) match {
        case Left(errors) => fail(errors.mkString(", "))
        case Right(user) => {
          user.name must be(Name(first = None, last = None))
        }
      }
    }

    "creates user organization asynchronously" in {
      val user = UsersDao.create(None, createUserForm()).right.get

      waitFor { () =>
        OrganizationsDao.findAll(Authorization.All, forUserGuid = Some(user.guid)).size == 1
      } must be(true)
    }

  }
}

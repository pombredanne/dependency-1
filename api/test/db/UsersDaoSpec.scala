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


  "findAll by guids" in {
    val user1 = UsersDao.create(None, createUserForm()).right.getOrElse {
      sys.error("Failed to create user")
    }

    val user2 = UsersDao.create(None, createUserForm()).right.getOrElse {
      sys.error("Failed to create user")
    }

    UsersDao.findAll(guids = Some(Seq(user1.guid, user2.guid))).map(_.guid) must be(
      Seq(user1.guid, user2.guid)
    )

    UsersDao.findAll(guids = Some(Nil)) must be(Nil)
    UsersDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    UsersDao.findAll(guids = Some(Seq(user1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(user1.guid))
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
        OrganizationsDao.findAll(forUserGuid = Some(user.guid)).size == 1
      } must be(true)
    }

  }
}

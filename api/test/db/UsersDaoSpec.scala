package db

import com.bryzek.dependency.v0.models.{NameForm, UserForm}
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class UsersDaoSpec @javax.inject.Inject() (
  helpers: Helpers,
  usersDao: UsersDao
) extends PlaySpec with OneAppPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "Special users" must {
    "anonymous user exists" in {
      usersDao.anonymousUser.email must be(
        usersDao.AnonymousEmailAddress
      )
    }

    "system user exists" in {
      usersDao.systemUser.email must be(
        usersDao.SystemEmailAddress
      )
    }

    "system and anonymous users are different" in {
      usersDao.AnonymousEmailAddress must not be(
        usersDao.SystemEmailAddress
      )

      usersDao.anonymousUser.guid must not be(
        usersDao.systemUser.guid
      )
    }

  }

  "findByEmail" in {
    usersDao.findByEmail(usersDao.SystemEmailAddress).map(_.email) must be(
      Some(usersDao.SystemEmailAddress)
    )

    usersDao.findByEmail(UUID.randomUUID.toString) must be(None)
  }

  "findByGuid" in {
    usersDao.findByGuid(usersDao.systemUser.guid).map(_.guid) must be(
      Some(usersDao.systemUser.guid)
    )

    usersDao.findByGuid(UUID.randomUUID) must be(None)
  }


  "findAll by guids" in {
    val user1 = usersDao.create(
      createdBy = None,
      valid = usersDao.validate(helpers.createUserForm())
    )

    val user2 = usersDao.create(
      createdBy = None,
      valid = usersDao.validate(helpers.createUserForm())
    )

    usersDao.findAll(guids = Some(Seq(user1.guid, user2.guid))).map(_.guid) must be(
      Seq(user1.guid, user2.guid)
    )

    usersDao.findAll(guids = Some(Nil)) must be(Nil)
    usersDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    usersDao.findAll(guids = Some(Seq(user1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(user1.guid))
  }

  "create" must {
    "user with email and name" in {
      val email = helpers.createTestEmail()
      val name = NameForm(
        first = Some("Michael"),
        last = Some("Bryzek")
      )
      val user = usersDao.create(
        createdBy = None,
        valid = usersDao.validate(
          helpers.createUserForm(
            email = email,
            name = Some(name)
          )
        )
      )
      user.email must be(email)
      user.name.flatMap(_.first) must be(name.first)
      user.name.flatMap(_.last) must be(name.last)
    }

    "processes empty name" in {
      val name = NameForm(
        first = Some("  "),
        last = Some("   ")
      )
      val user = usersDao.create(
        createdBy = None,
        valid = usersDao.validate(
          helpers.createUserForm().copy(name = Some(name))
        )
      )
      user.name must be(None)
    }

  }
}

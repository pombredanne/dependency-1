package db

import com.bryzek.dependency.v0.models.UserIdentifier
import io.flow.user.v0.models.User

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class UserIdentifiersDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createUserIdentifier(): (User, UserIdentifier) = {
    val user = createUser()
    val userIdentifier = UserIdentifiersDao.createForUser(systemUser, user)
    (user, userIdentifier)
  }

  "createForUser" in {
    val user = createUser()
    val identifier1 = UserIdentifiersDao.createForUser(systemUser, user)
    val identifier2 = UserIdentifiersDao.createForUser(systemUser, user)

    identifier1.value must not be(identifier2.value)
    identifier1.user.guid must be(user.guid)
    identifier2.user.guid must be(user.guid)
    identifier1.value.length must be(60)
  }

  "findByGuid" in {
    val (user, identifier) = createUserIdentifier()

    UserIdentifiersDao.findByGuid(Authorization.All, identifier.guid).map(_.guid) must be(
      Some(identifier.guid)
    )

    UserIdentifiersDao.findByGuid(Authorization.All, UUID.randomUUID) must be(None)
  }

  "findAll" must {
    "filter by guids" in {
      val (user1, identifier1) = createUserIdentifier()
      val (user2, identifier2) = createUserIdentifier()

      UserIdentifiersDao.findAll(Authorization.All, guids = Some(Seq(identifier1.guid, identifier2.guid))).map(_.guid).sorted must be(
        Seq(identifier1.guid, identifier2.guid).sorted
      )

      UserIdentifiersDao.findAll(Authorization.All, guids = Some(Nil)) must be(Nil)
      UserIdentifiersDao.findAll(Authorization.All, guids = Some(Seq(UUID.randomUUID))) must be(Nil)
      UserIdentifiersDao.findAll(Authorization.All, guids = Some(Seq(identifier1.guid, UUID.randomUUID))).map(_.guid) must be(
        Seq(identifier1.guid)
      )
    }

    "filter by identifier" in {
      val (user, identifier) = createUserIdentifier()

      UserIdentifiersDao.findAll(Authorization.All, value = Some(identifier.value)).map(_.guid) must be(Seq(identifier.guid))
      UserIdentifiersDao.findAll(Authorization.All, value = Some(createTestKey())) must be(Nil)
    }
  }
}

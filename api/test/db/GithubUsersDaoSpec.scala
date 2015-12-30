package db

import io.flow.user.v0.models.{Name, NameForm, UserForm}
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class GithubUsersDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "upsertById" in {
    val form = createGithubUserForm()
    val user1 = GithubUsersDao.create(None, form)

    val user2 = GithubUsersDao.upsertById(None, form)
    user1.guid must be(user2.guid)

    val user3 = GithubUsersDao.upsertById(Some(systemUser), createGithubUserForm())

    user2.guid must not be(user3.guid)
    user2.guid must not be(user3.guid)

    user1.audit.createdBy.id must be(db.UsersDao.anonymousUser.guid)
    user3.audit.createdBy.id must be(systemUser.guid)
  }

  "findByGuid" in {
    val user = createGithubUser()
    GithubUsersDao.findById(user.id).map(_.guid) must be(
      Some(user.id)
    )

    UsersDao.findById(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val user1 = createGithubUser()
    val user2 = createGithubUser()

    GithubUsersDao.findAll(guids = Some(Seq(user1.guid, user2.guid))).map(_.guid) must be(
      Seq(user1.guid, user2.guid)
    )

    GithubUsersDao.findAll(guids = Some(Nil)) must be(Nil)
    GithubUsersDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    GithubUsersDao.findAll(guids = Some(Seq(user1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(user1.guid))
  }

}

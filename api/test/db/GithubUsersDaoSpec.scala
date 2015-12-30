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
    user1.id must be(user2.id)

    val user3 = GithubUsersDao.upsertById(Some(systemUser), createGithubUserForm())

    user2.id must not be(user3.id)
    user2.id must not be(user3.id)

    user1.audit.createdBy.id must be(db.UsersDao.anonymousUser.id)
    user3.audit.createdBy.id must be(systemUser.id)
  }

  "findById" in {
    val user = createGithubUser()
    GithubUsersDao.findById(user.id).map(_.id) must be(
      Some(user.id)
    )

    UsersDao.findById(UUID.randomUUID) must be(None)
  }

  "findAll by ids" in {
    val user1 = createGithubUser()
    val user2 = createGithubUser()

    GithubUsersDao.findAll(ids = Some(Seq(user1.id, user2.id))).map(_.id) must be(
      Seq(user1.id, user2.id)
    )

    GithubUsersDao.findAll(ids = Some(Nil)) must be(Nil)
    GithubUsersDao.findAll(ids = Some(Seq(UUID.randomUUID))) must be(Nil)
    GithubUsersDao.findAll(ids = Some(Seq(user1.id, UUID.randomUUID))).map(_.id) must be(Seq(user1.id))
  }

}

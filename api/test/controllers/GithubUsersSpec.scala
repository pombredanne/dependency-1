package controllers

import db.GithubUsersDao
import com.bryzek.dependency.api.lib.MockGithubData
import com.bryzek.dependency.v0.Client
import com.bryzek.dependency.v0.models.GithubAuthenticationForm
import io.flow.play.util.Validation
import io.flow.github.v0.models.OwnerType
import io.flow.github.v0.models.{User => GithubUser}

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class GithubUsersSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createGithubUser(): GithubUser = {
    val login = createTestEmail()
    GithubUser(
      id = positiveRandomLong(),
      login = login,
      name = None,
      email = Some(login),
      avatarUrl = None,
      gravatarId = None,
      url = s"https://github.com/$login",
      htmlUrl = s"https://github.com/$login",
      `type` = OwnerType.User
    )
  }

  "POST /authentications/github with valid token" in new WithServer(port=port) {
    val githubUser = createGithubUser()
    val code = "test"

    MockGithubData.addUser(githubUser, code)
    val user = await(anonClient.githubUsers.postAuthenticationsAndGithub(GithubAuthenticationForm(code = code)))
    user.email must beEqualTo(githubUser.email)

    GithubUsersDao.findById(githubUser.id).map(_.user.id) must beEqualTo(Some(user.id))

    // Test idempotence
    val user2 = await(anonClient.githubUsers.postAuthenticationsAndGithub(GithubAuthenticationForm(code = code)))
    user2.email must beEqualTo(githubUser.email)
  }

  "POST /authentications/github accepts account w/out email" in new WithServer(port=port) {
    val githubUser = createGithubUser().copy(email = None)
    val code = "test"

    MockGithubData.addUser(githubUser, code)
    val user = await(
      anonClient.githubUsers.postAuthenticationsAndGithub(GithubAuthenticationForm(code = code))
    )
    user.email should be(None)
    db.UsersDao.findByGithubUserId(githubUser.id).map(_.guid) must beEqualTo(Some(user.id))
  }

}

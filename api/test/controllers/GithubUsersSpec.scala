package controllers

import db.GithubUsersDao
import com.bryzek.dependency.lib.MockGithubData
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

    MockGithubData.addUser(code, githubUser)
    val user = await(anonClient.githubUsers.postAuthenticationsAndGithub(GithubAuthenticationForm(code = code)))
    user.email must beEqualTo(githubUser.email)

    GithubUsersDao.findByLogin(githubUser.login).map(_.user.guid) must beEqualTo(Some(user.guid))
  }

  "POST /authentications/github validates accounts w/out email" in new WithServer(port=port) {
    val githubUser = createGithubUser().copy(email = None)
    val code = "test"

    MockGithubData.addUser(code, githubUser)
    expectErrors {
      anonClient.githubUsers.postAuthenticationsAndGithub(GithubAuthenticationForm(code = code))
    }.errors.map(_.message) must beEqualTo(Seq("Github account does not have an email address that we can read"))
  }

  /*
  "POST /users/authenticate validates non-existent email" in new WithServer(port=port) {
    Seq(createTestEmail(), "foo").foreach { email =>
      val response = expectErrors(
        anonClient.users.postAuthenticate(AuthenticationForm(email = email))
      )

      response.errors.map(_.code) must beEqualTo(
        Seq(Validation.Codes.UserAuthorizationFailed)
      )
      response.errors.map(_.message) must beEqualTo(
        Seq("Email address not found")
      )
    }
  }
 */

}

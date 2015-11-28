package controllers

import com.bryzek.dependency.v0.Client
import com.bryzek.dependency.v0.models.GithubAuthenticationForm
import io.flow.play.util.Validation

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class GithubUsersSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /authentications/github with valid token" in new WithServer(port=port) {
    val user = createUser()
    val token = "test"
    await(anonClient.githubUsers.postAuthenticationsAndGithub(GithubAuthenticationForm(token = token))).guid must beEqualTo(user.guid)
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

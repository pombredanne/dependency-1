package controllers

import com.bryzek.dependency.v0.Client
import com.bryzek.dependency.v0.models.AuthenticationForm
import io.flow.user.v0.models.{NameForm, UserForm}
import io.flow.play.util.Validation

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class UsersSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val user1 = createUser()
  lazy val user2 = createUser()

  "GET /users requires auth" in new WithServer(port=port) {
    expectNotAuthorized {
      anonClient.users.get()
    }
  }

  "GET /users/:guid" in new WithServer(port=port) {
    expectNotAuthorized {
      anonClient.users.getByGuid(UUID.randomUUID)
    }
  }

  "GET /users by guid" in new WithServer(port=port) {
    await(
      client.users.get(guid = Some(user1.guid))
    ).map(_.guid) must beEqualTo(
      Seq(user1.guid)
    )

    await(
      client.users.get(guid = Some(UUID.randomUUID))
    ).map(_.guid) must be(
      Nil
    )
  }

  "GET /users by email" in new WithServer(port=port) {
    await(
      client.users.get(email = user1.email)
    ).map(_.email) must beEqualTo(
      Seq(user1.email)
    )

    await(
      client.users.get(email = Some(UUID.randomUUID.toString))
    ) must be(
      Nil
    )
  }

  "GET /users/:guid" in new WithServer(port=port) {
    await(client.users.getByGuid(user1.guid)).guid must beEqualTo(user1.guid)
    await(client.users.getByGuid(user2.guid)).guid must beEqualTo(user2.guid)

    expectNotFound {
      client.users.getByGuid(UUID.randomUUID)
    }
  }

  "POST /users/authenticate with valid email" in new WithServer(port=port) {
    await(anonClient.users.postAuthenticate(AuthenticationForm(email = user1.email.get))).guid must beEqualTo(user1.guid)
    await(anonClient.users.postAuthenticate(AuthenticationForm(email = user2.email.get))).guid must beEqualTo(user2.guid)
  }

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

  "POST /users w/out name" in new WithServer(port=port) {
    val email = createTestEmail()
    val user = await(anonClient.users.post(UserForm(email = Some(email))))
    user.email must beEqualTo(Some(email))
    user.name.first must beEqualTo(None)
    user.name.last must beEqualTo(None)
  }

  "POST /users w/ name" in new WithServer(port=port) {
    val email = createTestEmail()
    val user = await(
      anonClient.users.post(
        UserForm(
          email = Some(email),
          name = Some(
            NameForm(first = Some("Michael"), last = Some("Bryzek"))
          )
        )
      )
    )
    user.email must beEqualTo(Some(email))
    user.name.first must beEqualTo(Some("Michael"))
    user.name.last must beEqualTo(Some("Bryzek"))
  }

  "POST /users validates duplicate email" in new WithServer(port=port) {
    expectErrors(
      anonClient.users.post(UserForm(email = Some(user1.email.get)))
    ).errors.map(_.message) must beEqualTo(
      Seq("Email is already registered")
    )
  }

  "POST /users validates empty email" in new WithServer(port=port) {
    expectErrors(
      anonClient.users.post(UserForm(email = Some("   ")))
    ).errors.map(_.message) must beEqualTo(
      Seq("Email address cannot be empty")
    )
  }

  "POST /users validates email address format" in new WithServer(port=port) {
    expectErrors(
      anonClient.users.post(UserForm(email = Some("mbfoo.com")))
    ).errors.map(_.message) must beEqualTo(
      Seq("Please enter a valid email address")
    )
  }

}

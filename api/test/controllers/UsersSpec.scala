package controllers

import com.bryzek.dependency.v0.Client
import com.bryzek.dependency.v0.models.{AuthenticationForm, NameForm, UserForm}
import io.flow.play.util.Validation

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class UsersSpec extends PlaySpecification with db.Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val port = 9010
  lazy val client = new Client(s"http://localhost:$port")

  lazy val user1 = createUser()
  lazy val user2 = createUser()

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
      client.users.get(email = Some(user1.email))
    ).map(_.email) must beEqualTo(
      Seq(user1.email)
    )

    await(
      client.users.get(email = Some(UUID.randomUUID.toString))
    ).map(_.email) must be(
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
    await(client.users.postAuthenticate(AuthenticationForm(email = user1.email))).guid must beEqualTo(user1.guid)
    await(client.users.postAuthenticate(AuthenticationForm(email = user2.email))).guid must beEqualTo(user2.guid)
  }

  "POST /users/authenticate validates non-existent email" in new WithServer(port=port) {
    Seq(createTestEmail(), "foo").foreach { email =>
      val response = expectErrors(
        client.users.postAuthenticate(AuthenticationForm(email = email))
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
    val user = await(client.users.post(UserForm(email = email)))
    user.email must beEqualTo(email)
    user.name.first must beEqualTo(None)
    user.name.last must beEqualTo(None)
  }

  "POST /users w/ name" in new WithServer(port=port) {
    val email = createTestEmail()
    val user = await(
      client.users.post(
        UserForm(
          email = email,
          name = Some(
            NameForm(first = Some("Michael"), last = Some("Bryzek"))
          )
        )
      )
    )
    user.email must beEqualTo(email)
    user.name.first must beEqualTo(Some("Michael"))
    user.name.last must beEqualTo(Some("Bryzek"))
  }

  "POST /users validates duplicate email" in new WithServer(port=port) {
    expectErrors(
      client.users.post(UserForm(email = user1.email))
    ).errors.map(_.message) must beEqualTo(
      Seq("Email is already registered")
    )
  }

}

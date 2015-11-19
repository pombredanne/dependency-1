package controllers

import com.bryzek.dependency.v0.{Authorization, Client}
import com.bryzek.dependency.v0.models.LanguageForm
import io.flow.play.util.Validation

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class LanguagesSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val language1 = createLanguage()
  lazy val language2 = createLanguage()

  "GET /languages by guid" in new WithServer(port=port) {
    await(
      client.languages.get(guid = Some(language1.guid))
    ).map(_.guid) must beEqualTo(
      Seq(language1.guid)
    )

    await(
      client.languages.get(guid = Some(UUID.randomUUID))
    ).map(_.guid) must be(
      Nil
    )
  }

  "GET /languages by name" in new WithServer(port=port) {
    await(
      client.languages.get(name = Some(language1.name.toString))
    ).map(_.name) must beEqualTo(
      Seq(language1.name)
    )

    await(
      client.languages.get(name = Some(language1.name.toString.toUpperCase))
    ).map(_.name) must beEqualTo(
      Seq(language1.name)
    )

    await(
      client.languages.get(name = Some(UUID.randomUUID.toString))
    ) must be(
      Nil
    )
  }

  "GET /languages/:guid" in new WithServer(port=port) {
    await(client.languages.getByGuid(language1.guid)).guid must beEqualTo(language1.guid)
    await(client.languages.getByGuid(language2.guid)).guid must beEqualTo(language2.guid)

    expectNotFound {
      client.languages.getByGuid(UUID.randomUUID)
    }
  }

  "POST /languages" in new WithServer(port=port) {
    val form = createLanguageForm()
    val language = await(client.languages.post(form))
    language.name.toString must beEqualTo(form.name)
  }

  "POST /languages validates duplicate name" in new WithServer(port=port) {
    expectErrors(
      client.languages.post(createLanguageForm().copy(name = language1.name.toString))
    ).errors.map(_.message) must beEqualTo(
      Seq("Language with this name already exists")
    )
  }

  "DELETE /languages" in new WithServer(port=port) {
    val language = createLanguage()
    await(
      client.languages.deleteByGuid(language.guid)
    ) must beEqualTo(())

    expectNotFound(
      client.languages.getByGuid(language.guid)
    )

    expectNotFound(
      client.languages.deleteByGuid(language.guid)
    )
  }

}

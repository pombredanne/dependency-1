package controllers

import com.bryzek.dependency.v0.{Authorization, Client}
import com.bryzek.dependency.v0.models.ProjectForm
import io.flow.play.util.Validation

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class LibrariesSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()
  lazy val library1 = createLibrary(org)()
  lazy val library2 = createLibrary(org)()

  "GET /libraries by guid" in new WithServer(port=port) {
    await(
      client.libraries.get(guid = Some(library1.guid))
    ).map(_.guid) must beEqualTo(
      Seq(library1.guid)
    )

    await(
      client.libraries.get(guid = Some(UUID.randomUUID))
    ).map(_.guid) must be(
      Nil
    )
  }

  "GET /libraries by groupId" in new WithServer(port=port) {
    await(
      client.libraries.get(groupId = Some(library1.groupId))
    ).map(_.groupId) must beEqualTo(
      Seq(library1.groupId)
    )

    await(
      client.libraries.get(groupId = Some(UUID.randomUUID.toString))
    ) must be(
      Nil
    )
  }

  "GET /libraries by artifactId" in new WithServer(port=port) {
    await(
      client.libraries.get(artifactId = Some(library1.artifactId))
    ).map(_.artifactId) must beEqualTo(
      Seq(library1.artifactId)
    )

    await(
      client.libraries.get(artifactId = Some(UUID.randomUUID.toString))
    ) must be(
      Nil
    )
  }

  "GET /libraries/:guid" in new WithServer(port=port) {
    await(client.libraries.getByGuid(library1.guid)).guid must beEqualTo(library1.guid)
    await(client.libraries.getByGuid(library2.guid)).guid must beEqualTo(library2.guid)

    expectNotFound {
      client.libraries.getByGuid(UUID.randomUUID)
    }
  }

  "POST /libraries" in new WithServer(port=port) {
    val form = createLibraryForm(org)()
    val library = await(client.libraries.post(form))
    library.groupId must beEqualTo(form.groupId)
    library.artifactId must beEqualTo(form.artifactId)
  }

  "POST /libraries validates duplicate" in new WithServer(port=port) {
    expectErrors(
      client.libraries.post(
        createLibraryForm(org)().copy(
          groupId = library1.groupId,
          artifactId = library1.artifactId
        )
      )
    ).errors.map(_.message) must beEqualTo(
      Seq("Library with this group id and artifact id already exists")
    )
  }

  "DELETE /libraries" in new WithServer(port=port) {
    val library = createLibrary(org)()
    await(
      client.libraries.deleteByGuid(library.guid)
    ) must beEqualTo(())

    expectNotFound(
      client.libraries.getByGuid(library.guid)
    )

    expectNotFound(
      client.libraries.deleteByGuid(library.guid)
    )
  }

}

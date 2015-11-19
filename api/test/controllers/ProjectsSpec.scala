package controllers

import com.bryzek.dependency.v0.Client
import com.bryzek.dependency.v0.models.ProjectForm
import io.flow.play.util.Validation

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class ProjectsSpec extends PlaySpecification with db.Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val port = 9010
  lazy val client = new Client(s"http://localhost:$port")

  lazy val project1 = createProject()
  lazy val project2 = createProject()

  "GET /projects by guid" in new WithServer(port=port) {
    await(
      client.projects.get(guid = Some(project1.guid))
    ).map(_.guid) must beEqualTo(
      Seq(project1.guid)
    )

    await(
      client.projects.get(guid = Some(UUID.randomUUID))
    ).map(_.guid) must be(
      Nil
    )
  }

  "GET /projects by name" in new WithServer(port=port) {
    await(
      client.projects.get(name = Some(project1.name))
    ).map(_.name) must beEqualTo(
      Seq(project1.name)
    )

    await(
      client.projects.get(name = Some(UUID.randomUUID.toString))
    ) must be(
      Nil
    )
  }

  "GET /projects/:guid" in new WithServer(port=port) {
    await(client.projects.getByGuid(project1.guid)).guid must beEqualTo(project1.guid)
    await(client.projects.getByGuid(project2.guid)).guid must beEqualTo(project2.guid)

    expectNotFound {
      client.projects.getByGuid(UUID.randomUUID)
    }
  }

  "POST /projects" in new WithServer(port=port) {
    val form = createProjectForm()
    val project = await(client.projects.post(form))
    project.name must beEqualTo(form.name)
    project.scms must beEqualTo(form.scms)
    project.uri must beEqualTo(form.uri)
  }

  "POST /projects validates duplicate name" in new WithServer(port=port) {
    expectErrors(
      client.projects.post(createProjectForm().copy(name = Some(project1.name.get)))
    ).errors.map(_.message) must beEqualTo(
      Seq("Name is already registered")
    )
  }

}

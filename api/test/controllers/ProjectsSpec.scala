package controllers

import com.bryzek.dependency.v0.{Authorization, Client}
import com.bryzek.dependency.v0.models.{ProjectForm, ProjectPatchForm}
import io.flow.play.util.Validation

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class ProjectsSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

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
      client.projects.get(name = Some(project1.name.toUpperCase))
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
      client.projects.post(createProjectForm().copy(name = project1.name))
    ).errors.map(_.message) must beEqualTo(
      Seq("Project with this name already exists")
    )
  }

  "PUT /projects/:guid" in new WithServer(port=port) {
    val form = createProjectForm()
    val project = createProject(form)
    val newUri = "test"
    await(client.projects.putByGuid(project.guid, form.copy(uri = newUri)))
    await(client.projects.getByGuid(project.guid)).uri must beEqualTo(newUri)
  }

  "PATCH /projects/:guid w/ no data leaves project unchanged" in new WithServer(port=port) {
    val project = createProject()
    await(client.projects.patchByGuid(project.guid, ProjectPatchForm()))
    val updated = await(client.projects.getByGuid(project.guid))
    updated.name must beEqualTo(project.name)
    updated.scms must beEqualTo(project.scms)
    updated.uri must beEqualTo(project.uri)
  }

  "PATCH /projects/:guid w/ name" in new WithServer(port=port) {
    val project = createProject()
    val newName = project.name + "2"
    await(client.projects.patchByGuid(project.guid, ProjectPatchForm(name = Some(newName))))
    await(client.projects.getByGuid(project.guid)).name must beEqualTo(newName)
  }

  "DELETE /projects" in new WithServer(port=port) {
    val project = createProject()
    await(
      client.projects.deleteByGuid(project.guid)
    ) must beEqualTo(())

    expectNotFound(
      client.projects.getByGuid(project.guid)
    )

    expectNotFound(
      client.projects.deleteByGuid(project.guid)
    )
  }

}

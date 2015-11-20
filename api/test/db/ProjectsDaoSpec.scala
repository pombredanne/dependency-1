package db

import com.bryzek.dependency.v0.models.Scms
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ProjectsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "findByName" in {
    val project = createProject()
    ProjectsDao.findByName(project.name).map(_.guid) must be(
      Some(project.guid)
    )

    ProjectsDao.findByName(UUID.randomUUID.toString) must be(None)
  }

  "findByGuid" in {
    val project = createProject()
    ProjectsDao.findByGuid(project.guid).map(_.guid) must be(
      Some(project.guid)
    )

    ProjectsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val project1 = createProject()
    val project2 = createProject()

    ProjectsDao.findAll(guids = Some(Seq(project1.guid, project2.guid))).map(_.guid) must be(
      Seq(project1.guid, project2.guid)
    )

    ProjectsDao.findAll(guids = Some(Nil)) must be(Nil)
    ProjectsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    ProjectsDao.findAll(guids = Some(Seq(project1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(project1.guid))
  }

  "update" in {
    val form = createProjectForm()
    val project = createProject(form)
    ProjectsDao.update(systemUser, project, ProjectsDao.validate(form.copy(uri = "test"), Some(project)))
    ProjectsDao.findByGuid(project.guid).map(_.uri) must be(Some("test"))
  }

  "update allows name change" in {
    val form = createProjectForm()
    val project = createProject(form)
    val newName = project.name + "2"
    val updated = ProjectsDao.update(systemUser, project, ProjectsDao.validate(form.copy(name = newName), Some(project)))
    updated.guid must be(project.guid)
    updated.name must be(newName)
  }

  "create" must {
    "validates SCMS" in {
      val form = createProjectForm().copy(scms = Scms.UNDEFINED("other"))
      ProjectsDao.validate(form).errors.map(_.message) must be(
        Seq("Scms not found")
      )
    }

    "validates empty name" in {
      val form = createProjectForm().copy(name = "   ")
      ProjectsDao.validate(form).errors.map(_.message) must be(
        Seq("Name cannot be empty")
      )
    }

    "validates duplicate names" in {
      val project = createProject()
      val form = createProjectForm().copy(name = project.name.toString.toUpperCase)
      ProjectsDao.validate(form).errors.map(_.message) must be(
        Seq("Project with this name already exists")
      )
    }

    "validates empty uri" in {
      val form = createProjectForm().copy(uri = "   ")
      ProjectsDao.validate(form).errors.map(_.message) must be(
        Seq("Uri cannot be empty")
      )
    }

  }

}

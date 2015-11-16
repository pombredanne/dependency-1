package db

import com.bryzek.dependency.v0.models.Scms
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ProjectsDaoSpec @javax.inject.Inject() (
  helpers: Helpers,
  projectsDao: ProjectsDao
) extends PlaySpec with OneAppPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "findByName" in {
    val project = helpers.createProject()
    projectsDao.findByName(project.name).map(_.guid) must be(
      Some(project.guid)
    )

    projectsDao.findByName(UUID.randomUUID.toString) must be(None)
  }

  "findByGuid" in {
    val project = helpers.createProject()
    projectsDao.findByGuid(project.guid).map(_.guid) must be(
      Some(project.guid)
    )

    projectsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val project1 = helpers.createProject()
    val project2 = helpers.createProject()

    projectsDao.findAll(guids = Some(Seq(project1.guid, project2.guid))).map(_.guid) must be(
      Seq(project1.guid, project2.guid)
    )

    projectsDao.findAll(guids = Some(Nil)) must be(Nil)
    projectsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    projectsDao.findAll(guids = Some(Seq(project1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(project1.guid))
  }

  "create" must {
    "validates SCMS" in {
      val form = helpers.createProjectForm().copy(scms = Scms.UNDEFINED("other"))
      projectsDao.validate(form).errors.map(_.message) must be(
        Seq("Scms not found")
      )
    }

    "validates empty name" in {
      val form = helpers.createProjectForm().copy(name = "   ")
      projectsDao.validate(form).errors.map(_.message) must be(
        Seq("Name cannot be empty")
      )
    }

    "validates duplicate names" in {
      val project = helpers.createProject()
      val form = helpers.createProjectForm().copy(name = project.name.toString.toUpperCase)
      projectsDao.validate(form).errors.map(_.message) must be(
        Seq("Project with this name already exists")
      )
    }
  }

}

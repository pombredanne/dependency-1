package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ProjectLibrariesDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()
  lazy val project = createProject(org)
  lazy val projectLibrary = createProjectLibrary(project)

  "validate" must {

  }

  "create" in {
    val projectLibrary = createProjectLibrary(project) (
      createProjectLibraryForm(project).copy(crossBuildVersion = Some("2.11"))
    )

    val one = ProjectLibrariesDao.findByGuid(Authorization.All, projectLibrary.guid).getOrElse {
      sys.error("Failed to create project library")
    }

    one.project.guid must be(project.guid)
    one.groupId must be(projectLibrary.groupId)
    one.artifactId must be(projectLibrary.artifactId)
    one.version must be(projectLibrary.version)
    one.crossBuildVersion must be(Some("2.11"))
    one.path must be(projectLibrary.path)
  }

  "upsert" in {
    val form = createProjectLibraryForm(project)

    val one = create(ProjectLibrariesDao.upsert(systemUser, form))
    one.crossBuildVersion must be(None)
    create(ProjectLibrariesDao.upsert(systemUser, form)).guid must be(one.guid)

    val two = create(ProjectLibrariesDao.upsert(systemUser, form.copy(crossBuildVersion = Some("2.10"))))
    two.crossBuildVersion must be(Some("2.10"))
    create(ProjectLibrariesDao.upsert(systemUser, form.copy(crossBuildVersion = Some("2.10")))).guid must be(two.guid)

    val three = create(ProjectLibrariesDao.upsert(systemUser, form.copy(crossBuildVersion = Some("2.11"))))
    three.crossBuildVersion must be(Some("2.11"))
    create(ProjectLibrariesDao.upsert(systemUser, form.copy(crossBuildVersion = Some("2.11")))).guid must be(three.guid)

    val other = create(ProjectLibrariesDao.upsert(systemUser, form.copy(groupId = form.groupId + ".other")))
    other.groupId must be(form.groupId + ".other")
  }

  "softDelete" in {
    val projectLibrary = createProjectLibrary(project)
    ProjectLibrariesDao.softDelete(systemUser, projectLibrary)
    ProjectLibrariesDao.findByGuid(Authorization.All, projectLibrary.guid) must be(None)
  }

  "findAll" must {

    "filter by guid" in {
      ProjectLibrariesDao.findAll(Authorization.All, guid = Some(projectLibrary.guid)).map(_.guid) must be(
        Seq(projectLibrary.guid)
      )
      ProjectLibrariesDao.findAll(Authorization.All, projectGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "filter by guids" in {
      val other = createProjectLibrary(project)

      ProjectLibrariesDao.findAll(Authorization.All, guids = Some(Seq(projectLibrary.guid, other.guid))).map(_.guid).sorted must be(
        Seq(projectLibrary.guid, other.guid).sorted
      )

      ProjectLibrariesDao.findAll(Authorization.All, guids = Some(Seq(projectLibrary.guid, UUID.randomUUID))).map(_.guid).sorted must be(
        Seq(projectLibrary.guid).sorted
      )

      ProjectLibrariesDao.findAll(Authorization.All, guids = Some(Nil)) must be(Nil)
    }

    "filter by projectGuid" in {
      ProjectLibrariesDao.findAll(Authorization.All, guid = Some(projectLibrary.guid), projectGuid = Some(project.guid)).map(_.guid) must be(
        Seq(projectLibrary.guid)
      )
      ProjectLibrariesDao.findAll(Authorization.All, projectGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "filter by groupId" in {
      ProjectLibrariesDao.findAll(Authorization.All, guid = Some(projectLibrary.guid), groupId = Some(projectLibrary.groupId)).map(_.guid) must be(
        Seq(projectLibrary.guid)
      )
      ProjectLibrariesDao.findAll(Authorization.All, groupId = Some(UUID.randomUUID.toString)) must be(Nil)
    }

    "filter by artifactId" in {
      ProjectLibrariesDao.findAll(Authorization.All, guid = Some(projectLibrary.guid), artifactId = Some(projectLibrary.artifactId)).map(_.guid) must be(
        Seq(projectLibrary.guid)
      )
      ProjectLibrariesDao.findAll(Authorization.All, artifactId = Some(UUID.randomUUID.toString)) must be(Nil)
    }

    "filter by version" in {
      ProjectLibrariesDao.findAll(Authorization.All, guid = Some(projectLibrary.guid), version = Some(projectLibrary.version)).map(_.guid) must be(
        Seq(projectLibrary.guid)
      )
      ProjectLibrariesDao.findAll(Authorization.All, version = Some(UUID.randomUUID.toString)) must be(Nil)
    }

    "filter by crossBuildVersion" in {
      val projectLibrary = createProjectLibrary(project) (
        createProjectLibraryForm(project).copy(crossBuildVersion = Some("2.11"))
      )

      ProjectLibrariesDao.findAll(Authorization.All, guid = Some(projectLibrary.guid), crossBuildVersion = Some(Some("2.11"))).map(_.guid) must be(
        Seq(projectLibrary.guid)
      )

      ProjectLibrariesDao.findAll(Authorization.All, guid = Some(projectLibrary.guid), crossBuildVersion = Some(Some(UUID.randomUUID.toString))) must be(Nil)
      ProjectLibrariesDao.findAll(Authorization.All, guid = Some(projectLibrary.guid), crossBuildVersion = Some(None)) must be(Nil)
    }

  }

}

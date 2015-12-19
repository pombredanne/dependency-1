package db

import com.bryzek.dependency.v0.models.{BinaryType, SyncEvent}
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ProjectBinariesDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()
  lazy val project = createProject(org)
  lazy val projectBinary = createProjectBinary(project)

  "validate" must {

    "catch empty name" in {
      ProjectBinariesDao.validate(
        systemUser,
        createProjectBinaryForm(project).copy(name = BinaryType.UNDEFINED("   "))
      ) must be(Seq("Name cannot be empty"))
    }

    "catch empty version" in {
      ProjectBinariesDao.validate(
        systemUser,
        createProjectBinaryForm(project).copy(version = "   ")
      ) must be(Seq("Version cannot be empty"))
    }

    "catch invalid project" in {
      ProjectBinariesDao.validate(
        systemUser,
        createProjectBinaryForm(project).copy(projectGuid = UUID.randomUUID())
      ) must be(Seq("Project not found"))
    }

    "catch project we cannot access" in {
      ProjectBinariesDao.validate(
        createUser(),
        createProjectBinaryForm(project)
      ) must be(Seq("You are not authorized to edit this project"))
    }

  }

  "create" in {
    val form = createProjectBinaryForm(project)
    val projectBinary = createProjectBinary(project)(form)

    val one = ProjectBinariesDao.findByGuid(Authorization.All, projectBinary.guid).getOrElse {
      sys.error("Failed to create project binary")
    }

    one.project.guid must be(project.guid)
    one.name must be(projectBinary.name)
    one.version must be(projectBinary.version)
    one.path must be(projectBinary.path)
  }

  "upsert" in {
    val form = createProjectBinaryForm(project)

    val one = create(ProjectBinariesDao.upsert(systemUser, form))
    create(ProjectBinariesDao.upsert(systemUser, form)).guid must be(one.guid)
    create(ProjectBinariesDao.upsert(systemUser, form)).guid must be(one.guid)
  }

  "setBinary" in {
    val projectBinary = createProjectBinary(project)
    val binary = createBinary(org)
    ProjectBinariesDao.setBinary(systemUser, projectBinary, binary)
    ProjectBinariesDao.findByGuid(Authorization.All, projectBinary.guid).flatMap(_.binary.map(_.guid)) must be(Some(binary.guid))
  }

  "softDelete" in {
    val projectBinary = createProjectBinary(project)
    ProjectBinariesDao.softDelete(systemUser, projectBinary)
    ProjectBinariesDao.findByGuid(Authorization.All, projectBinary.guid) must be(None)
  }

  "findAll" must {

    "filter by guid" in {
      ProjectBinariesDao.findAll(Authorization.All, guid = Some(projectBinary.guid)).map(_.guid) must be(
        Seq(projectBinary.guid)
      )
      ProjectBinariesDao.findAll(Authorization.All, projectGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "filter by guids" in {
      val other = createProjectBinary(project)

      ProjectBinariesDao.findAll(Authorization.All, guids = Some(Seq(projectBinary.guid, other.guid))).map(_.guid).sorted must be(
        Seq(projectBinary.guid, other.guid).sorted
      )

      ProjectBinariesDao.findAll(Authorization.All, guids = Some(Seq(projectBinary.guid, UUID.randomUUID))).map(_.guid).sorted must be(
        Seq(projectBinary.guid).sorted
      )

      ProjectBinariesDao.findAll(Authorization.All, guids = Some(Nil)) must be(Nil)
    }

    "filter by projectGuid" in {
      ProjectBinariesDao.findAll(Authorization.All, guid = Some(projectBinary.guid), projectGuid = Some(project.guid)).map(_.guid) must be(
        Seq(projectBinary.guid)
      )
      ProjectBinariesDao.findAll(Authorization.All, projectGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "filter by binaryGuid" in {
      val binary = createBinary(org)
      val projectBinary = createProjectBinary(project)
      ProjectBinariesDao.setBinary(systemUser, projectBinary, binary)

      ProjectBinariesDao.findAll(Authorization.All, guid = Some(projectBinary.guid), binaryGuid = Some(binary.guid)).map(_.guid) must be(
        Seq(projectBinary.guid)
      )
      ProjectBinariesDao.findAll(Authorization.All, binaryGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "filter by name" in {
      ProjectBinariesDao.findAll(Authorization.All, guid = Some(projectBinary.guid), name = Some(projectBinary.name)).map(_.guid) must be(
        Seq(projectBinary.guid)
      )
      ProjectBinariesDao.findAll(Authorization.All, name = Some(UUID.randomUUID.toString)) must be(Nil)
    }

    "filter by version" in {
      ProjectBinariesDao.findAll(Authorization.All, guid = Some(projectBinary.guid), version = Some(projectBinary.version)).map(_.guid) must be(
        Seq(projectBinary.guid)
      )
      ProjectBinariesDao.findAll(Authorization.All, version = Some(UUID.randomUUID.toString)) must be(Nil)
    }

    "filter by isSynced" in {
      createSync(createSyncForm(objectGuid = projectBinary.guid, event = SyncEvent.Completed))

      ProjectBinariesDao.findAll(Authorization.All, guid = Some(projectBinary.guid), isSynced = Some(true)).map(_.guid) must be(
        Seq(projectBinary.guid)
      )
      ProjectBinariesDao.findAll(Authorization.All, guid = Some(projectBinary.guid), isSynced = Some(false)) must be(Nil)
    }

    "filter by hasBinary" in {
      val projectBinary = createProjectBinary(project)

      ProjectBinariesDao.findAll(Authorization.All, guid = Some(projectBinary.guid), hasBinary = Some(true)) must be(Nil)
      ProjectBinariesDao.findAll(Authorization.All, guid = Some(projectBinary.guid), hasBinary = Some(false)).map(_.guid) must be(
        Seq(projectBinary.guid)
      )

      ProjectBinariesDao.setBinary(systemUser, projectBinary, createBinary(org))

      ProjectBinariesDao.findAll(Authorization.All, guid = Some(projectBinary.guid), hasBinary = Some(true)).map(_.guid) must be(
        Seq(projectBinary.guid)
      )
      ProjectBinariesDao.findAll(Authorization.All, guid = Some(projectBinary.guid), hasBinary = Some(false)) must be(Nil)
    }
  }


}

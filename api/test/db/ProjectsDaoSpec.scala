package db

import com.bryzek.dependency.v0.models.{BinaryVersion, LibraryVersion, Project, Scms, VersionForm, Visibility}
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ProjectsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()
  lazy val project1 = createProject(org)
  lazy val project2 = createProject(org)

  "findByOrganizationGuidAndName" in {
    ProjectsDao.findByOrganizationGuidAndName(Authorization.All, org.guid, project1.name).map(_.guid) must be(
      Some(project1.guid)
    )

    ProjectsDao.findByOrganizationGuidAndName(Authorization.All, UUID.randomUUID, project1.name) must be(None)
    ProjectsDao.findByOrganizationGuidAndName(Authorization.All, org.guid, UUID.randomUUID.toString) must be(None)
  }

  "findByGuid" in {
    ProjectsDao.findByGuid(Authorization.All, project1.guid).map(_.guid) must be(
      Some(project1.guid)
    )

    ProjectsDao.findByGuid(Authorization.All, UUID.randomUUID) must be(None)
  }

  "update" in {
    val form = createProjectForm(org)
    val project = createProject(org)(form)
    ProjectsDao.update(systemUser, project, form.copy(uri = "http://github.com/mbryzek/test"))
    ProjectsDao.findByGuid(Authorization.All, project.guid).map(_.uri) must be(Some("http://github.com/mbryzek/test"))
  }

  "update allows name change" in {
    val form = createProjectForm(org)
    val project = createProject(org)(form)
    val newName = project.name + "2"
    val updated = ProjectsDao.update(systemUser, project, form.copy(name = newName)).right.get
    updated.guid must be(project.guid)
    updated.name must be(newName)
  }

  "create" must {
    "validates SCMS" in {
      val form = createProjectForm(org).copy(scms = Scms.UNDEFINED("other"))
      ProjectsDao.create(systemUser, form) must be(Left(Seq("Scms not found")))
    }

    "validates SCMS URI" in {
      val form = createProjectForm(org).copy(scms = Scms.Github, uri = "http://github.com/mbryzek")
      ProjectsDao.create(systemUser, form) must be(
        Left(Seq("Invalid uri path[http://github.com/mbryzek] missing project name"))
      )
    }

    "validates empty name" in {
      val form = createProjectForm(org).copy(name = "   ")
      ProjectsDao.create(systemUser, form) must be(Left(Seq("Name cannot be empty")))
    }

    "validates duplicate names" in {
      val project = createProject(org)
      val form = createProjectForm(org).copy(name = project.name.toString.toUpperCase)
      ProjectsDao.create(systemUser, form) must be(Left(Seq("Project with this name already exists")))
      ProjectsDao.validate(systemUser, form, existing = Some(project)) must be(Nil)

      val org2 = createOrganization()
      val form2 = createProjectForm(org2).copy(name = project.name)
      ProjectsDao.validate(systemUser, form2) must be(Nil)
    }

    "validates empty uri" in {
      val form = createProjectForm(org).copy(uri = "   ")
      ProjectsDao.create(systemUser, form) must be(Left(Seq("Uri cannot be empty")))
    }

  }

  "setDependencies" must {

    "set binaries" in {
      val project = createProject(org)()
      val binary = createBinaryForm()
      ProjectsDao.setDependencies(systemUser, project, binaries = Some(Seq(binary)))
      BinariesDao.findAll(Authorization.All, projectGuid = Some(project.guid)).map(_.name.toString) must be(Seq(binary.name.toString))

      // Make sure we can reset w/out error
      ProjectsDao.setDependencies(systemUser, project, binaries = Some(Seq(binary)))
    }

    "set binaries can upgrade version" in {
      val project = createProject(org)()
      val binary = createBinaryForm().copy(version = "2.11.6")
      ProjectsDao.setDependencies(systemUser, project, binaries = Some(Seq(binary)))
      BinaryVersionsDao.findAll(projectGuid = Some(project.guid)).map(_.version.toString) must be(Seq("2.11.6"))

      ProjectsDao.setDependencies(systemUser, project, binaries = Some(Seq(binary.copy(version = "2.11.7"))))
      BinaryVersionsDao.findAll(projectGuid = Some(project.guid)).map(_.version.toString) must be(Seq("2.11.7"))
    }

  }

  "findAll" must {

    "guids" in {
      ProjectsDao.findAll(Authorization.All, guids = Some(Seq(project1.guid, project2.guid))).map(_.guid) must be(
        Seq(project1.guid, project2.guid)
      )

      ProjectsDao.findAll(Authorization.All, guids = Some(Nil)) must be(Nil)
      ProjectsDao.findAll(Authorization.All, guids = Some(Seq(UUID.randomUUID))) must be(Nil)
      ProjectsDao.findAll(Authorization.All, guids = Some(Seq(project1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(project1.guid))
    }

    "name" in {
      ProjectsDao.findAll(Authorization.All, name = Some(project1.name.toUpperCase)).map(_.guid) must be(
        Seq(project1.guid)
      )

      ProjectsDao.findAll(Authorization.All, name = Some(UUID.randomUUID.toString)).map(_.guid) must be(Nil)
    }

    "organizationGuid" in {
      ProjectsDao.findAll(Authorization.All, guid = Some(project1.guid), organizationGuid = Some(org.guid)).map(_.guid) must be(
        Seq(project1.guid)
      )

      ProjectsDao.findAll(Authorization.All, guid = Some(project1.guid), organizationGuid = Some(createOrganization().guid)) must be(Nil)
    }

    "org" in {
      ProjectsDao.findAll(Authorization.All, guid = Some(project1.guid), org = Some(org.key)).map(_.guid) must be(
        Seq(project1.guid)
      )

      ProjectsDao.findAll(Authorization.All, guid = Some(project1.guid), org = Some(createOrganization().key)) must be(Nil)
    }

    "with library" must {

      "groupId" in {
        val (project, version) = createProjectWithLibrary(org)()

        ProjectsDao.findAll(Authorization.All, groupId = Some(version.library.groupId)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(Authorization.All, groupId = Some(UUID.randomUUID.toString)).map(_.guid) must be(Nil)
      }

      "artifactId" in {
        val (project, version) = createProjectWithLibrary(org)()

        ProjectsDao.findAll(Authorization.All, artifactId = Some(version.library.artifactId)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(Authorization.All, artifactId = Some(UUID.randomUUID.toString)).map(_.guid) must be(Nil)
      }

      "version" in {
        val (project, version) = createProjectWithLibrary(org)()

        ProjectsDao.findAll(Authorization.All, version = Some(version.version)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(Authorization.All, version = Some(UUID.randomUUID.toString)).map(_.guid) must be(Nil)
      }

      "libraryGuid" in {
        val (project, version) = createProjectWithLibrary(org)()

        ProjectsDao.findAll(Authorization.All, libraryGuid = Some(version.library.guid)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(Authorization.All, libraryGuid = Some(UUID.randomUUID)).map(_.guid) must be(Nil)
      }

      "libraryVersionGuid" in {
        val (project, version) = createProjectWithLibrary(org)()

        ProjectsDao.findAll(Authorization.All, libraryVersionGuid = Some(version.guid)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(Authorization.All, libraryVersionGuid = Some(UUID.randomUUID)).map(_.guid) must be(Nil)
      }
    }

    "with binary" must {

      "binary name" in {
        val (project, version) = createProjectWithBinary(org)

        ProjectsDao.findAll(Authorization.All, binary = Some(version.binary.name.toString)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(Authorization.All, binary = Some(UUID.randomUUID.toString)) must be(Nil)
      }

      "binary guid" in {
        val (project, version) = createProjectWithBinary(org)

        ProjectsDao.findAll(Authorization.All, binaryGuid = Some(version.binary.guid)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(Authorization.All, binaryGuid = Some(UUID.randomUUID)) must be(Nil)
      }

      "binary version guid" in {
        val (project, version) = createProjectWithBinary(org)

        ProjectsDao.findAll(Authorization.All, binaryVersionGuid = Some(version.guid)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(Authorization.All, binaryVersionGuid = Some(UUID.randomUUID)) must be(Nil)
      }

    }

    "authorization for public projects" in {
      val user = createUser()
      val org = createOrganization(user = user)
      val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Public))

      ProjectsDao.findAll(Authorization.PublicOnly, guid = Some(project.guid)).map(_.guid) must be(Seq(project.guid))
      ProjectsDao.findAll(Authorization.All, guid = Some(project.guid)).map(_.guid) must be(Seq(project.guid))
      ProjectsDao.findAll(Authorization.Organization(org.guid), guid = Some(project.guid)).map(_.guid) must be(Seq(project.guid))
      ProjectsDao.findAll(Authorization.Organization(createOrganization().guid), guid = Some(project.guid)).map(_.guid) must be(Seq(project.guid))
      ProjectsDao.findAll(Authorization.User(user.guid), guid = Some(project.guid)).map(_.guid) must be(Seq(project.guid))
    }

    "authorization for private projects" in {
      val user = createUser()
      val org = createOrganization(user = user)
      val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Private))

      ProjectsDao.findAll(Authorization.PublicOnly, guid = Some(project.guid)) must be(Nil)
      ProjectsDao.findAll(Authorization.All, guid = Some(project.guid)).map(_.guid) must be(Seq(project.guid))
      ProjectsDao.findAll(Authorization.Organization(org.guid), guid = Some(project.guid)).map(_.guid) must be(Seq(project.guid))
      ProjectsDao.findAll(Authorization.Organization(createOrganization().guid), guid = Some(project.guid)) must be(Nil)
      ProjectsDao.findAll(Authorization.User(user.guid), guid = Some(project.guid)).map(_.guid) must be(Seq(project.guid))
      ProjectsDao.findAll(Authorization.User(createUser().guid), guid = Some(project.guid)) must be(Nil)
    }

  }

}



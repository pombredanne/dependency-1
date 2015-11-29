package db

import com.bryzek.dependency.v0.models.{LanguageVersion, LibraryVersion, Project, Scms, VersionForm}
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ProjectsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val project1 = createProject()
  lazy val project2 = createProject()

  "findByName" in {
    ProjectsDao.findByName(project1.name).map(_.guid) must be(
      Some(project1.guid)
    )

    ProjectsDao.findByName(UUID.randomUUID.toString) must be(None)
  }

  "findByGuid" in {
    ProjectsDao.findByGuid(project1.guid).map(_.guid) must be(
      Some(project1.guid)
    )

    ProjectsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "update" in {
    val form = createProjectForm()
    val project = createProject(form)
    ProjectsDao.update(systemUser, project, form.copy(uri = "http://github.com/mbryzek/test"))
    ProjectsDao.findByGuid(project.guid).map(_.uri) must be(Some("http://github.com/mbryzek/test"))
  }

  "update allows name change" in {
    val form = createProjectForm()
    val project = createProject(form)
    val newName = project.name + "2"
    val updated = ProjectsDao.update(systemUser, project, form.copy(name = newName)).right.get
    updated.guid must be(project.guid)
    updated.name must be(newName)
  }

  "create" must {
    "validates SCMS" in {
      val form = createProjectForm().copy(scms = Scms.UNDEFINED("other"))
      ProjectsDao.create(systemUser, form) must be(Left(Seq("Scms not found")))
    }

    "validates SCMS URI" in {
      val form = createProjectForm().copy(scms = Scms.Github, uri = "http://github.com/mbryzek")
      ProjectsDao.create(systemUser, form) must be(
        Left(Seq("Invalid uri path[http://github.com/mbryzek] missing project name"))
      )
    }

    "validates empty name" in {
      val form = createProjectForm().copy(name = "   ")
      ProjectsDao.create(systemUser, form) must be(Left(Seq("Name cannot be empty")))
    }

    "validates duplicate names" in {
      val project = createProject()
      val form = createProjectForm().copy(name = project.name.toString.toUpperCase)
      ProjectsDao.create(systemUser, form) must be(Left(Seq("Project with this name already exists")))
    }

    "validates empty uri" in {
      val form = createProjectForm().copy(uri = "   ")
      ProjectsDao.create(systemUser, form) must be(Left(Seq("Uri cannot be empty")))
    }

  }

  "setDependencies" must {

    "set languages" in {
      val project = createProject()
      val language = createLanguageForm()
      ProjectsDao.setDependencies(systemUser, project, languages = Some(Seq(language)))
      LanguagesDao.findAll(projectGuid = Some(project.guid)).map(_.name.toString) must be(Seq(language.name.toString))

      // Make sure we can reset w/out error
      ProjectsDao.setDependencies(systemUser, project, languages = Some(Seq(language)))
    }

    "set languages can upgrade version" in {
      val project = createProject()
      val language = createLanguageForm().copy(version = "2.11.6")
      ProjectsDao.setDependencies(systemUser, project, languages = Some(Seq(language)))
      LanguageVersionsDao.findAll(projectGuid = Some(project.guid)).map(_.version.toString) must be(Seq("2.11.6"))

      ProjectsDao.setDependencies(systemUser, project, languages = Some(Seq(language.copy(version = "2.11.7"))))
      LanguageVersionsDao.findAll(projectGuid = Some(project.guid)).map(_.version.toString) must be(Seq("2.11.7"))
    }

    "set libraries" in {
      val project = createProject()
      val library = createLibraryForm()

      ProjectsDao.setDependencies(systemUser, project, libraries = Some(Seq(library)))
      val fetched = LibrariesDao.findAll(projectGuid = Some(project.guid)) match {
        case one :: Nil => one
        case _ => sys.error("Expected one library")
      }
      fetched.groupId must be(library.groupId)
      fetched.artifactId must be(library.artifactId)

      ProjectsDao.setDependencies(systemUser, project, libraries = Some(Seq(library)))
    }

    "set libraries can upgrade version" in {
      val project = createProject()
      val versionForm = VersionForm(
        version = "1.4.0"
      )
      val library = createLibraryForm().copy(version = Some(versionForm))
      ProjectsDao.setDependencies(systemUser, project, libraries = Some(Seq(library)))
      LibraryVersionsDao.findAll(projectGuid = Some(project.guid)).map(_.version.toString) must be(Seq("1.4.0"))

      ProjectsDao.setDependencies(systemUser, project, libraries = Some(Seq(library.copy(version = Some(versionForm.copy("1.4.2"))))))
      LibraryVersionsDao.findAll(projectGuid = Some(project.guid)).map(_.version.toString) must be(Seq("1.4.2"))
    }

  }

  "findAll" must {

    "guids" in {
      ProjectsDao.findAll(guids = Some(Seq(project1.guid, project2.guid))).map(_.guid) must be(
        Seq(project1.guid, project2.guid)
      )

      ProjectsDao.findAll(guids = Some(Nil)) must be(Nil)
      ProjectsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
      ProjectsDao.findAll(guids = Some(Seq(project1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(project1.guid))
    }

    "name" in {
      ProjectsDao.findAll(name = Some(project1.name.toUpperCase)).map(_.guid) must be(
        Seq(project1.guid)
      )

      ProjectsDao.findAll(name = Some(UUID.randomUUID.toString)).map(_.guid) must be(Nil)
    }

    "with library" must {

      "groupId" in {
        val (project, version) = createProjectWithLibrary()

        ProjectsDao.findAll(groupId = Some(version.library.groupId)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(groupId = Some(UUID.randomUUID.toString)).map(_.guid) must be(Nil)
      }

      "artifactId" in {
        val (project, version) = createProjectWithLibrary()

        ProjectsDao.findAll(artifactId = Some(version.library.artifactId)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(artifactId = Some(UUID.randomUUID.toString)).map(_.guid) must be(Nil)
      }

      "version" in {
        val (project, version) = createProjectWithLibrary()

        ProjectsDao.findAll(version = Some(version.version)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(version = Some(UUID.randomUUID.toString)).map(_.guid) must be(Nil)
      }

      "libraryGuid" in {
        val (project, version) = createProjectWithLibrary()

        ProjectsDao.findAll(libraryGuid = Some(version.library.guid)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(libraryGuid = Some(UUID.randomUUID)).map(_.guid) must be(Nil)
      }

      "libraryVersionGuid" in {
        val (project, version) = createProjectWithLibrary()

        ProjectsDao.findAll(libraryVersionGuid = Some(version.guid)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(libraryVersionGuid = Some(UUID.randomUUID)).map(_.guid) must be(Nil)
      }
    }

    "with language" must {

      "language name" in {
        val (project, version) = createProjectWithLanguage()

        ProjectsDao.findAll(language = Some(version.language.name.toString)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(language = Some(UUID.randomUUID.toString)) must be(Nil)
      }

      "language guid" in {
        val (project, version) = createProjectWithLanguage()

        ProjectsDao.findAll(languageGuid = Some(version.language.guid)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(languageGuid = Some(UUID.randomUUID)) must be(Nil)
      }

      "language version guid" in {
        val (project, version) = createProjectWithLanguage()

        ProjectsDao.findAll(languageVersionGuid = Some(version.guid)).map(_.guid) must be(
          Seq(project.guid)
        )

        ProjectsDao.findAll(languageVersionGuid = Some(UUID.randomUUID)) must be(Nil)
      }

    }
   }
}

package db

import com.bryzek.dependency.v0.models._
import com.bryzek.dependency.v0.models.VersionForm
import io.flow.user.v0.models.{NameForm, User, UserForm}
import java.util.UUID

trait Helpers {

  lazy val systemUser = UsersDao.systemUser

  def createTestEmail(): String = {
    s"z-test-${UUID.randomUUID}@test.bryzek.com"
  }

  def createTestName(): String = {
    s"Z Test ${UUID.randomUUID}"
  }

  def createLanguage(
    form: LanguageForm = createLanguageForm()
  ): Language = {
    LanguagesDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create language")
    }
  }

  def createLanguageForm() = LanguageForm(
    name = s"z-test-language-${UUID.randomUUID}".toLowerCase,
    version = "0.0.1"
  )

  def createLanguageVersion(
    language: Language = createLanguage(),
    version: String = s"0.0.1-${UUID.randomUUID}".toLowerCase
  ): LanguageVersion = {
    LanguageVersionsDao.create(systemUser, language.guid, version)
  }

  def createLibrary(
    form: LibraryForm = createLibraryForm()
  ): Library = {
    LibrariesDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create library")
    }
  }

  def createLibraryForm() = LibraryForm(
    resolvers = Seq("http://dependencies.io.flow"),
    groupId = s"z-test.${UUID.randomUUID}".toLowerCase,
    artifactId = s"z-test-${UUID.randomUUID}".toLowerCase,
    version = Some(VersionForm("0.0.1"))
  )

  def createLibraryVersion(
    library: Library = createLibrary(),
    version: VersionForm = createVersionForm()
  ): LibraryVersion = {
    LibraryVersionsDao.create(systemUser, library.guid, version)
  }

  def createVersionForm(
    version: String = s"0.0.1-${UUID.randomUUID}".toLowerCase,
    crossBuildVersion: Option[String] = None
  ) = {
    VersionForm(version, crossBuildVersion)
  }

  def createProject(
    form: ProjectForm = createProjectForm()
  ): Project = {
    ProjectsDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create projet")
    }
  }

  def createProjectForm() = {
    val name = createTestName()
    ProjectForm(
      name = name,
      scms = Scms.GitHub,
      uri = s"http://github.com/test/${UUID.randomUUID}"
    )
  }

  def createProjectWithLibrary(): (Project, LibraryVersion) = {
    val libraryForm = createLibraryForm().copy(
      groupId = UUID.randomUUID.toString,
      artifactId = UUID.randomUUID.toString,
      version = Some(createVersionForm())
    )

    val project = createProject()
    ProjectsDao.setDependencies(systemUser, project, libraries = Some(Seq(libraryForm)))

    val library = LibrariesDao.findByResolversAndGroupIdAndArtifactId(libraryForm.resolvers, libraryForm.groupId, libraryForm.artifactId).getOrElse {
      sys.error("Failed to find library")
    }

    val libraryVersion = LibraryVersionsDao.findByLibraryAndVersionAndCrossBuildVersion(library, libraryForm.version.get.version, libraryForm.version.get.crossBuildVersion).getOrElse {
      sys.error("Failed to find library version")
    }

    (project, libraryVersion)
  }

  def createProjectWithLanguage(): (Project, LanguageVersion) = {
    val languageForm = createLanguageForm().copy(
      name = createTestName(),
      version = UUID.randomUUID.toString
    )

    val project = createProject()
    ProjectsDao.setDependencies(systemUser, project, languages = Some(Seq(languageForm)))

    val language = LanguagesDao.findByName(languageForm.name).getOrElse {
      sys.error("Failed to find language")
    }
    println(s"Created languages - $language")

    val languageVersion = LanguageVersionsDao.findByLanguageAndVersion(language, languageForm.version).getOrElse {
      sys.error("Failed to find language version")
    }

    (project, languageVersion)
  }

  def createUser(
    form: UserForm = createUserForm()
  ): User = {
    UsersDao.create(None, form).right.getOrElse {
      sys.error("Failed to create user")
    }
  }

  def createUserForm(
    email: String = createTestEmail(),
    name: Option[NameForm] = None
  ) = UserForm(
    email = Some(email),
    name = name
  )

}

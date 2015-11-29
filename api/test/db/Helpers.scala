package db

import com.bryzek.dependency.v0.models._
import io.flow.user.v0.models.{NameForm, User, UserForm}
import java.util.UUID
import scala.util.Random

trait Helpers {

  lazy val systemUser = UsersDao.systemUser

  def createTestEmail(): String = {
    s"z-test-${UUID.randomUUID}@test.bryzek.com"
  }

  def createTestName(): String = {
    s"Z Test ${UUID.randomUUID}"
  }

  @scala.annotation.tailrec
  final def positiveRandomLong(): Long = {
    val value = (new Random()).nextLong
    (value > 0) match {
      case true => value
      case false => positiveRandomLong()
    }
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

  def createLibraryForm(
    versionForm: VersionForm = VersionForm("0.0.1")
  ) = LibraryForm(
    groupId = s"z-test.${UUID.randomUUID}".toLowerCase,
    artifactId = s"z-test-${UUID.randomUUID}".toLowerCase,
    version = Some(versionForm)
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
      scms = Scms.Github,
      uri = s"http://github.com/test/${UUID.randomUUID}"
    )
  }

  def createProjectWithLibrary(
    libraryForm: LibraryForm = createLibraryForm().copy(
      groupId = s"z-test-${UUID.randomUUID}".toLowerCase,
      artifactId = s"z-test-${UUID.randomUUID}".toLowerCase,
      version = Some(createVersionForm())
    )
  ): (Project, LibraryVersion) = {
    val project = createProject()
    ProjectsDao.setDependencies(systemUser, project, libraries = Some(Seq(libraryForm)))

    val library = LibrariesDao.findByGroupIdAndArtifactId(libraryForm.groupId, libraryForm.artifactId).getOrElse {
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

  def createGithubUser(
    form: GithubUserForm = createGithubUserForm()
  ): GithubUser = {
    GithubUsersDao.create(None, form)
  }

  def createGithubUserForm(
    user: User = createUser(),
    id: Long = positiveRandomLong(),
    login: String = createTestEmail()
  ) = {
    GithubUserForm(
      userGuid = user.guid,
      id = id,
      login = login
    )
  }

  def createToken(
    form: TokenForm = createTokenForm()
  ): Token = {
    TokensDao.create(systemUser, form)
  }

  def createTokenForm(
    user: User = createUser(),
    tag: String = createTestName().toLowerCase,
    token: String = UUID.randomUUID().toString.toLowerCase
  ) = {
    TokenForm(
      userGuid = user.guid,
      tag = tag,
      token = token
    )
  }

  def createResolver(
    form: ResolverForm = createResolverForm()
  ): Resolver = {
    ResolversDao.create(systemUser, form)
  }

  def createResolverForm(
    user: User = createUser(),
    uri: String = s"http://${UUID.randomUUID}.z-test.flow.io"
  ) = {
    ResolverForm(
      userGuid = user.guid,
      uri = uri
    )
  }

  def createWatchProject(
    form: WatchProjectForm = createWatchProjectForm()
  ): WatchProject = {
    WatchProjectsDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create watch project")
    }
  }

  def createWatchProjectForm(
    user: User = createUser(),
    project: Project = createProject()
  ) = {
    WatchProjectForm(
      userGuid = user.guid,
      projectGuid = project.guid
    )
  }

}

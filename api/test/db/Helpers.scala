package db

import com.bryzek.dependency.v0.models._
import java.util.UUID

object Helpers {

  lazy val systemUser = UsersDao.systemUser

  def createTestEmail(): String = {
    s"z-test-${UUID.randomUUID}@test.bryzek.com"
  }

  def createLanguage(
    form: LanguageForm = createLanguageForm()
  ): Language = {
    LanguagesDao.create(systemUser, LanguagesDao.validate(form))
  }

  def createLanguageForm() = LanguageForm(
    name = s"z-test-language-${UUID.randomUUID}".toLowerCase,
    version = None
  )

  def createLanguageVersion(
    language: Language = Helpers.createLanguage(),
    version: String = s"0.0.1-${UUID.randomUUID}".toLowerCase
  ): LanguageVersion = {
    LanguageVersionsDao.upsert(systemUser, language.guid, version)
  }

  def createLibrary(
    form: LibraryForm = createLibraryForm()
  ): Library = {
    LibrariesDao.create(systemUser, LibrariesDao.validate(form))
  }

  def createLibraryForm() = LibraryForm(
    resolvers = Seq("http://dependencies.io.flow"),
    groupId = s"z-test.${UUID.randomUUID}".toLowerCase,
    artifactId = "z-test",
    version = None
  )

  def createLibraryVersion(
    library: Library = Helpers.createLibrary(),
    version: String = s"0.0.1-${UUID.randomUUID}".toLowerCase
  ): LibraryVersion = {
    LibraryVersionsDao.upsert(systemUser, library.guid, version)
  }

  def createProject(
    form: ProjectForm = createProjectForm()
  ): Project = {
    ProjectsDao.create(systemUser, ProjectsDao.validate(form))
  }

  def createProjectForm() = {
    val name = s"z-test-${UUID.randomUUID}"
    ProjectForm(
      name = name,
      scms = Scms.GitHub,
      uri = "http://github.com/test/${name}"
    )
  }

  def createUser(
    form: UserForm
  ): User = {
    UsersDao.create(None, UsersDao.validate(form))
  }

  def createUserForm(
    email: String = createTestEmail(),
    name: Option[NameForm] = None
  ) = UserForm(
    email = email,
    name = name
  )

}

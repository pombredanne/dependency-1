package db

import com.bryzek.dependency.v0.models._
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
    LanguagesDao.create(systemUser, LanguagesDao.validate(form))
  }

  def createLanguageForm() = LanguageForm(
    name = s"z-test-language-${UUID.randomUUID}".toLowerCase,
    version = None
  )

  def createLanguageVersion(
    language: Language = createLanguage(),
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
    artifactId = s"z-test-${UUID.randomUUID}".toLowerCase,
    version = None
  )

  def createLibraryVersion(
    library: Library = createLibrary(),
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
    val name = createTestName()
    ProjectForm(
      name = name,
      scms = Scms.GitHub,
      uri = "http://github.com/test/${UUID.randomUUID}"
    )
  }

  def createUser(
    form: UserForm = createUserForm()
  ): User = {
    UsersDao.create(None, UsersDao.validate(form))
  }

  def createUserForm(
    email: String = createTestEmail(),
    name: Option[NameForm] = None
  ) = UserForm(
    email = Some(email),
    name = name
  )

}

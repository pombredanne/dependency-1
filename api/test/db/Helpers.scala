package db

import com.bryzek.dependency.v0.models._
import java.util.UUID

@javax.inject.Singleton
class Helpers @javax.inject.Inject() (
  usersDao: UsersDao,
  projectsDao: ProjectsDao,
  languagesDao: LanguagesDao,
  languageVersionsDao: LanguageVersionsDao,
  librariesDao: LibrariesDao,
  libraryVersionsDao: LibraryVersionsDao
) {

  lazy val systemUser = usersDao.systemUser

  def createTestEmail(): String = {
    s"z-test-${UUID.randomUUID}@test.bryzek.com"
  }

  def createLanguage(
    form: LanguageForm = createLanguageForm()
  ): Language = {
    languagesDao.create(systemUser, languagesDao.validate(form))
  }

  def createLanguageForm() = LanguageForm(
    name = s"z-test-language-${UUID.randomUUID}".toLowerCase,
    version = None
  )

  def createLanguageVersion(
    language: Language = createLanguage(),
    version: String = s"0.0.1-${UUID.randomUUID}".toLowerCase
  ): LanguageVersion = {
    languageVersionsDao.upsert(systemUser, language.guid, version)
  }

  def createLibrary(
    form: LibraryForm = createLibraryForm()
  ): Library = {
    librariesDao.create(systemUser, librariesDao.validate(form))
  }

  def createLibraryForm() = LibraryForm(
    resolvers = Seq("http://dependencies.io.flow"),
    groupId = s"z-test.${UUID.randomUUID}".toLowerCase,
    artifactId = "z-test",
    version = None
  )

  def createLibraryVersion(
    library: Library = createLibrary(),
    version: String = s"0.0.1-${UUID.randomUUID}".toLowerCase
  ): LibraryVersion = {
    libraryVersionsDao.upsert(systemUser, library.guid, version)
  }

  def createProject(
    form: ProjectForm = createProjectForm()
  ): Project = {
    projectsDao.create(systemUser, projectsDao.validate(form))
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
    usersDao.create(None, usersDao.validate(form))
  }

  def createUserForm(
    email: String = createTestEmail(),
    name: Option[NameForm] = None
  ) = UserForm(
    email = email,
    name = name
  )

}

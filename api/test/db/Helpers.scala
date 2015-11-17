package db

import com.bryzek.dependency.v0.models._
import com.bryzek.dependency.v0.errors.{ErrorsResponse, UnitResponse}
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait Helpers {

  val DefaultDuration = Duration(5, TimeUnit.SECONDS)

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
    artifactId = "z-test",
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
    val name = s"z-test-${UUID.randomUUID}"
    ProjectForm(
      name = name,
      scms = Scms.GitHub,
      uri = "http://github.com/test/${name}"
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
    email = email,
    name = name
  )

  def expectMyErrors[T](
    f: => Future[T],
    duration: Duration = DefaultDuration
  ): ErrorsResponse = {
    Try(
      Await.result(f, duration)
    ) match {
      case Success(response) => {
        sys.error("Expected function to fail but it succeeded with: " + response)
      }
      case Failure(ex) =>  ex match {
        case e: ErrorsResponse => {
          e
        }
        case e => {
          sys.error(s"Expected an exception of type[ErrorsResponse] but got[$e]")
        }
      }
    }
  }

  def expectNotFound[T](
    f: => Future[T],
    duration: Duration = DefaultDuration
  ) {
    expectStatus(404) {
      Await.result(f, duration)
    }
  }

  def expectStatus(code: Int)(f: => Unit) {
    assert(code >= 400, s"code[$code] must be >= 400")

    Try(
      f
    ) match {
      case Success(response) => {
        org.specs2.execute.Failure(s"Expected HTTP[$code] but got HTTP 2xx")
      }
      case Failure(ex) => ex match {
        case UnitResponse(code) => {
          org.specs2.execute.Success()
        }
        case e => {
          org.specs2.execute.Failure(s"Unexpected error: $e")
        }
      }
    }
  }
}

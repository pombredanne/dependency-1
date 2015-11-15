package db

import com.bryzek.dependency.v0.models.{Language, LanguageForm, Library, LibraryForm}
import java.util.UUID

object Helpers {

  lazy val systemUser = UsersDao.systemUser

  def createLanguage(
    form: LanguageForm = createLanguageForm()
  ): Language = {
    LanguagesDao.create(systemUser, LanguagesDao.validate(form))
  }

  def createLanguageForm() = LanguageForm(
    name = s"z-test-language-${UUID.randomUUID}".toLowerCase,
    version = None
  )

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

}

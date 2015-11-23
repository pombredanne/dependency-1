package db

import com.bryzek.dependency.v0.models.{Project, LanguageForm, LanguageVersion, LanguageRecommendation}
import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._

class LanguageRecommendationsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createLanguageWithMultipleVersions(
    versions: Seq[String] = Seq("1.0.0", "1.0.1", "1.0.2")
  ): Seq[LanguageVersion] = {
    val language = createLanguage(createLanguageForm().copy(version = versions.head))
    versions.drop(1).map { version =>
      createLanguageVersion(
        language = language,
        version = version
      )
    }
    LanguageVersionsDao.findAll(languageGuid = Some(language.guid), limit = versions.size).reverse
  }

  def addLanguageVersion(project: Project, languageVersion: LanguageVersion) {
    ProjectsDao.setDependencies(
      systemUser,
      project,
      languages = Some(
        Seq(
          LanguageForm(
            name = languageVersion.language.name.toString,
            version = languageVersion.version
          )
        )
      )
    )
  }
/*
  "no-op if nothing to upgrade" in {
    val project = createProject()
    LanguageRecommendationsDao.forProject(project) must be(Nil)
  }

  "ignores earlier versions of language" in {
    val languageVersions = createLanguageWithMultipleVersions()
    val project = createProject()
    addLanguageVersion(project, languageVersions.last)
    LanguageRecommendationsDao.forProject(project) must be(Nil)
  }

  "with language to upgrade" in {
    val languageVersions = createLanguageWithMultipleVersions()
    val project = createProject()
    addLanguageVersion(project, languageVersions.head)
    LanguageRecommendationsDao.forProject(project) must be(
      Seq(
        LanguageRecommendation(
          from = languageVersions.head,
          to = languageVersions.last
        )
      )
    )
  }
*/
  "Prefers latest production release even when more recent beta release is available" in {
    val languageVersions = createLanguageWithMultipleVersions(
      versions = Seq("1.0.0", "1.0.2-RC1", "1.0.1")
    )
    val project = createProject()
    addLanguageVersion(project, languageVersions.head)
    LanguageRecommendationsDao.forProject(project) must be(
      Seq(
        LanguageRecommendation(
          from = languageVersions.head,
          to = languageVersions.last
        )
      )
    )
  }

}

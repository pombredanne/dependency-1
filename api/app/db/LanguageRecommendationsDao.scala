package db

import com.bryzek.dependency.lib.Recommendations
import com.bryzek.dependency.v0.models.{LanguageRecommendation, LanguageVersion, Project, VersionForm}
import io.flow.play.postgresql.Pager
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

object LanguageRecommendationsDao {

  def forProject(project: Project): Seq[LanguageRecommendation] = {
    var versions = scala.collection.mutable.ListBuffer[LanguageRecommendation]()
    Pager.eachPage { offset =>
      LanguageVersionsDao.findAll(projectGuid = Some(project.guid), offset = offset)
    } { currentVersion =>
      recommend(currentVersion, versionsGreaterThan(currentVersion)).map { v =>
        versions ++= Seq(
          LanguageRecommendation(
            from = currentVersion,
            to = v,
            latest = LanguageVersionsDao.findAll(languageGuid = Some(currentVersion.language.guid), limit = 1).head
          )
        )
      }
    }
    versions
  }

  def recommend(currentVersion: LanguageVersion, others: Seq[LanguageVersion]): Option[LanguageVersion] = {
    Recommendations.version(
      VersionForm(currentVersion.version),
      others.map(v => VersionForm(v.version))
    ).map { form =>
      others.find { v => v.version == form.version }.getOrElse {
        sys.error(s"Failed to find language tag[$form]")
      }
    }
  }

  /**
   * Returns all versions of a language greater than the one specified
   */
  def versionsGreaterThan(languageVersion: LanguageVersion): Seq[LanguageVersion] = {
    var versions = scala.collection.mutable.ListBuffer[LanguageVersion]()
    Pager.eachPage { offset =>
      LanguageVersionsDao.findAll(
        languageGuid = Some(languageVersion.language.guid),
        greaterThanVersion = Some(languageVersion),
        offset = offset
      )
    } { languageVersion =>
      versions ++= Seq(languageVersion)
    }
    versions
  }
    

}

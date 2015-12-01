package db

import com.bryzek.dependency.lib.Recommendations
import com.bryzek.dependency.v0.models.{BinaryRecommendation, BinaryVersion, Project, VersionForm}
import io.flow.play.postgresql.Pager
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

object BinaryRecommendationsDao {

  def forProject(project: Project): Seq[BinaryRecommendation] = {
    var versions = scala.collection.mutable.ListBuffer[BinaryRecommendation]()
    Pager.eachPage { offset =>
      BinaryVersionsDao.findAll(projectGuid = Some(project.guid), offset = offset)
    } { currentVersion =>
      recommend(currentVersion, versionsGreaterThan(currentVersion)).map { v =>
        versions ++= Seq(
          BinaryRecommendation(
            from = currentVersion,
            to = v,
            latest = BinaryVersionsDao.findAll(binaryGuid = Some(currentVersion.binary.guid), limit = 1).headOption.getOrElse(v)
          )
        )
      }
    }
    versions
  }

  def recommend(currentVersion: BinaryVersion, others: Seq[BinaryVersion]): Option[BinaryVersion] = {
    Recommendations.version(
      VersionForm(currentVersion.version),
      others.map(v => VersionForm(v.version))
    ).map { version =>
      others.find { _.version == version }.getOrElse {
        sys.error(s"Failed to find binary tag[$version]")
      }
    }
  }

  /**
   * Returns all versions of a binary greater than the one specified
   */
  def versionsGreaterThan(binaryVersion: BinaryVersion): Seq[BinaryVersion] = {
    var versions = scala.collection.mutable.ListBuffer[BinaryVersion]()
    Pager.eachPage { offset =>
      BinaryVersionsDao.findAll(
        binaryGuid = Some(binaryVersion.binary.guid),
        greaterThanVersion = Some(binaryVersion),
        offset = offset
      )
    } { binaryVersion =>
      versions ++= Seq(binaryVersion)
    }
    versions
  }
    

}

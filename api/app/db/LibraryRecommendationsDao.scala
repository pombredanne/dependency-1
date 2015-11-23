package db

import com.bryzek.dependency.lib.Recommendations
import com.bryzek.dependency.v0.models.{LibraryRecommendation, LibraryVersion, Project}
import io.flow.play.postgresql.Pager
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

object LibraryRecommendationsDao {

  def forProject(project: Project): Seq[LibraryRecommendation] = {
    var versions = scala.collection.mutable.ListBuffer[LibraryRecommendation]()
    Pager.eachPage { offset =>
      LibraryVersionsDao.findAll(projectGuid = Some(project.guid), offset = offset)
    } { currentVersion =>
      recommend(currentVersion, versionsGreaterThan(currentVersion)).map { v =>
        versions ++= Seq(
          LibraryRecommendation(
            from = currentVersion,
            to = v
          )
        )
      }
    }
    versions
  }

  def recommend(currentVersion: LibraryVersion, others: Seq[LibraryVersion]): Option[LibraryVersion] = {
    Recommendations.version(currentVersion.version, others.map(_.version)).map { tag =>
      others.find(_.version == tag).getOrElse {
        sys.error(s"Failed to find tag[$tag]")
      }
    }
  }

  /**
   * Returns all versions of a library greater than the one specified
   */
  def versionsGreaterThan(libraryVersion: LibraryVersion): Seq[LibraryVersion] = {
    var versions = scala.collection.mutable.ListBuffer[LibraryVersion]()
    Pager.eachPage { offset =>
      LibraryVersionsDao.findAll(
        libraryGuid = Some(libraryVersion.library.guid),
        greaterThanVersion = Some(libraryVersion),
        offset = offset
      )
    } { libraryVersion =>
      versions ++= Seq(libraryVersion)
    }
    versions
  }
    

}

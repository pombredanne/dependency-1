package db

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
      versionsGreaterThan(currentVersion).headOption.map { v =>
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

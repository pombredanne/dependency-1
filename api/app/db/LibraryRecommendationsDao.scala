package db

import com.bryzek.dependency.api.lib.Recommendations
import com.bryzek.dependency.v0.models.{Library, LibraryVersion, Project, ProjectLibrary, VersionForm}
import io.flow.play.postgresql.Pager
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

case class LibraryRecommendation(
  library: Library,
  from: String,
  to: LibraryVersion,
  latest: LibraryVersion
)

object LibraryRecommendationsDao {

  def forProject(project: Project): Seq[LibraryRecommendation] = {
    var recommendations = scala.collection.mutable.ListBuffer[LibraryRecommendation]()

    Pager.eachPage { offset =>
      ProjectLibrariesDao.findAll(
        Authorization.Organization(project.organization.guid),
        projectGuid = Some(project.guid),
        hasLibrary = Some(true),
        offset = offset
      )
    } { projectLibrary =>
      projectLibrary.library.flatMap { lib => LibrariesDao.findByGuid(Authorization.All, lib.guid) }.map { library =>
        println("")
        println("")

        println(s"project[${project.name}]")
        println(s"  -- library ${library.groupId}.${library.artifactId} version[${projectLibrary.version}]")

        println("")
        println("")

        val recentVersions = versionsGreaterThan(library, projectLibrary.version)
        recommend(projectLibrary, recentVersions).map { v =>
          recommendations ++= Seq(
            LibraryRecommendation(
              library = library,
              from = projectLibrary.version,
              to = v,
              latest = recentVersions.lastOption.getOrElse(v)
            )
          )
        }
      }
    }

    recommendations
  }

  def recommend(current: ProjectLibrary, others: Seq[LibraryVersion]): Option[LibraryVersion] = {
    Recommendations.version(
      VersionForm(current.version, current.crossBuildVersion),
      others.map(v => VersionForm(v.version, v.crossBuildVersion))
    ).map { version =>
      others.find { _.version == version }.getOrElse {
        sys.error(s"Failed to find recommended library with version[$version]")
      }
    }
  }

  /**
   * Returns all versions of a library greater than the one specified
   */
  private[this] def versionsGreaterThan(library: Library, version: String): Seq[LibraryVersion] = {
    var recommendations = scala.collection.mutable.ListBuffer[LibraryVersion]()
    Pager.eachPage { offset =>
      LibraryVersionsDao.findAll(
        libraryGuid = Some(library.guid),
        greaterThanVersion = Some(version),
        offset = offset
      )
    } { libraryVersion =>
      recommendations ++= Seq(libraryVersion)
    }
    recommendations
  }

}

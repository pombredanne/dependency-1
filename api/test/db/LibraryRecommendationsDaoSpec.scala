package db

import com.bryzek.dependency.v0.models.{Project, LibraryForm, LibraryVersion, LibraryRecommendation, VersionForm}
import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._

class LibraryRecommendationsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createLibraryWithMultipleVersions(
    versions: Seq[String] = Seq("1.0.0", "1.0.1", "1.0.2")
  ): Seq[LibraryVersion] = {
    val library = createLibrary(createLibraryForm().copy(version = None))
    versions.map { version =>
      createLibraryVersion(
        library = library,
        version = VersionForm(version = version)
      )
    }
  }

  def addLibraryVersion(project: Project, libraryVersion: LibraryVersion) {
    ProjectsDao.setDependencies(
      systemUser,
      project,
      libraries = Some(
        Seq(
          LibraryForm(
            resolvers = libraryVersion.library.resolvers,
            groupId = libraryVersion.library.groupId,
            artifactId = libraryVersion.library.artifactId,
            version = Some(VersionForm(version = libraryVersion.version))
          )
        )
      )
    )
  }

  "no-op if nothing to upgrade" in {
    val project = createProject()
    LibraryRecommendationsDao.forProject(project) must be(Nil)
  }

  "ignores earlier versions of library" in {
    val libraryVersions = createLibraryWithMultipleVersions()
    val project = createProject()
    addLibraryVersion(project, libraryVersions.last)
    LibraryRecommendationsDao.forProject(project) must be(Nil)
  }

  "with library to upgrade" in {
    val libraryVersions = createLibraryWithMultipleVersions()
    val project = createProject()
    addLibraryVersion(project, libraryVersions.head)
    LibraryRecommendationsDao.forProject(project) must be(
      Seq(
        LibraryRecommendation(
          from = libraryVersions.head,
          to = libraryVersions.last,
          latest = libraryVersions.last
        )
      )
    )
  }

  "Prefers latest production release even when more recent beta release is available" in {
    val libraryVersions = createLibraryWithMultipleVersions(
      versions = Seq("1.0.0", "1.0.2-RC1", "1.0.1")
    )
    val project = createProject()
    addLibraryVersion(project, libraryVersions.head)
    LibraryRecommendationsDao.forProject(project) must be(
      Seq(
        LibraryRecommendation(
          from = libraryVersions.find(_.version == "1.0.0n").get,
          to = libraryVersions.find(_.version == "1.0.1").get,
          latest = libraryVersions.find(_.version == "1.0.2-RC1").get
        )
      )
    )
  }

  // TODO: Add tests specific to cross build versions

}

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
          to = libraryVersions.last
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
          from = libraryVersions.head,
          to = libraryVersions.last
        )
      )
    )
  }

  "recommendTag" must {

    "No recommendation if others is empty" in {
      LibraryRecommendationsDao.recommendTag("1.0.0", Nil) must be(None)
    }

    "No recommendation if others is self" in {
      LibraryRecommendationsDao.recommendTag("1.0.0", Seq("1.0.0")) must be(None)
    }

    "No recommendation if others are lower than self" in {
      LibraryRecommendationsDao.recommendTag("1.0.0", Seq("0.1.0", "0.1.1")) must be(None)
    }

    "No recommendation if greater versions are beta versions" in {
      LibraryRecommendationsDao.recommendTag("1.0.0", Seq("1.0.1-rc1")) must be(None)
    }

    "Recommends similar library if qualifiers match" in {
      LibraryRecommendationsDao.recommendTag(
        "9.4-1201-jdbc41", Seq("9.4-1205-jdbc4", "9.4-1205-jdbc41", "9.4-1205-jdbc42")
      ) must be(Some("9.4-1205-jdbc41"))
    }

  }

}

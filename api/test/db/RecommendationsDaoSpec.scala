package db

import com.bryzek.dependency.v0.models.{Project, LibraryForm, LibraryVersion, LibraryRecommendation}
import com.bryzek.dependency.v0.models.{Organization, Recommendation, VersionForm}
import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._

class RecommendationsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createRecommendation(
    org: Organization
  ): Recommendation = {
    val libraryVersions = createLibraryWithMultipleVersions(org)()
    val project = createProject(org)()
    addLibraryVersion(project, libraryVersions.head)
    RecommendationsDao.sync(systemUser, project)
    RecommendationsDao.findAll(projectGuid = Some(project.guid)).headOption.getOrElse {
      sys.error("Failed to create recommendation")
    }
  }

  lazy val org = createOrganization()

  "softDelete" in {
    val rec = createRecommendation(org)
    RecommendationsDao.softDelete(systemUser, rec)
    RecommendationsDao.findAll(projectGuid = Some(rec.project.guid)) must be(Nil)
  }

  "no-op if nothing to upgrade" in {
    val project = createProject(org)()
    RecommendationsDao.sync(systemUser, project)
    RecommendationsDao.findAll(projectGuid = Some(project.guid)) must be(Nil)
  }


  "ignores earlier versions of library" in {
    val libraryVersions = createLibraryWithMultipleVersions(org)()
    val project = createProject(org)()
    addLibraryVersion(project, libraryVersions.last)
    RecommendationsDao.sync(systemUser, project)
    RecommendationsDao.findAll(projectGuid = Some(project.guid)) must be(Nil)
  }

  "with library to upgrade" in {
    val rec = createRecommendation(org)
    RecommendationsDao.findAll(projectGuid = Some(rec.project.guid)).map(rec => (rec.from, rec.to)) must be(
      Seq(
        ("1.0.0", "1.0.2")
      )
    )
  }

  "Prefers latest production release even when more recent beta release is available" in {
    val libraryVersions = createLibraryWithMultipleVersions(org)(
      versions = Seq("1.0.0", "1.0.2-RC1", "1.0.1")
    )
    val project = createProject(org)()
    addLibraryVersion(project, libraryVersions.head)
    LibraryRecommendationsDao.forProject(project) must be(
      Seq(
        LibraryRecommendation(
          from = libraryVersions.find(_.version == "1.0.0").get,
          to = libraryVersions.find(_.version == "1.0.1").get,
          latest = libraryVersions.find(_.version == "1.0.2-RC1").get
        )
      )
    )
  }

}

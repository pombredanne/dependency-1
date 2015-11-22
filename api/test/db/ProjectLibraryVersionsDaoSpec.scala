package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ProjectLibraryVersionsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val (project, version) = createProjectWithLibrary()

  "findAll" must {

    "projectGuid" in {
      val one = ProjectLibraryVersionsDao.findAll(projectGuid = Some(project.guid)) match {
        case one :: Nil => one
        case recs => fail(s"expected 1 record but got[${recs.size}]")
      }
      one.project.guid must be(project.guid)
      one.libraryVersion.guid must be(version.guid)

      ProjectLibraryVersionsDao.findAll(projectGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "libraryGuid" in {
      val one = ProjectLibraryVersionsDao.findAll(libraryGuid = Some(version.library.guid)) match {
        case one :: Nil => one
        case recs => fail(s"expected 1 record but got[${recs.size}]")
      }
      one.project.guid must be(project.guid)
      one.libraryVersion.guid must be(version.guid)

      ProjectLibraryVersionsDao.findAll(libraryGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "libraryVersionGuid" in {
      val one = ProjectLibraryVersionsDao.findAll(libraryVersionGuid = Some(version.guid)) match {
        case one :: Nil => one
        case recs => fail(s"expected 1 record but got[${recs.size}]")
      }
      one.project.guid must be(project.guid)
      one.libraryVersion.guid must be(version.guid)

      ProjectLibraryVersionsDao.findAll(libraryVersionGuid = Some(UUID.randomUUID)) must be(Nil)
    }

  }

}

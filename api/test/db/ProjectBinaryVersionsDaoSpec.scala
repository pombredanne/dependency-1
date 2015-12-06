package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ProjectBinaryVersionsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val (project, version) = createProjectWithBinary()

  "findAll" must {

    "projectGuid" in {
      val one = ProjectBinaryVersionsDao.findAll(projectGuid = Some(project.guid)) match {
        case one :: Nil => one
        case recs => fail(s"expected 1 record but got[${recs.size}]")
      }
      one.project.guid must be(project.guid)
      one.binaryVersion.guid must be(version.guid)

      ProjectBinaryVersionsDao.findAll(projectGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "binaryGuid" in {
      val one = ProjectBinaryVersionsDao.findAll(binaryGuid = Some(version.binary.guid)) match {
        case one :: Nil => one
        case recs => fail(s"expected 1 record but got[${recs.size}]")
      }
      one.project.guid must be(project.guid)
      one.binaryVersion.guid must be(version.guid)

      ProjectBinaryVersionsDao.findAll(binaryGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "binaryVersionGuid" in {
      val one = ProjectBinaryVersionsDao.findAll(binaryVersionGuid = Some(version.guid)) match {
        case one :: Nil => one
        case recs => fail(s"expected 1 record but got[${recs.size}]")
      }
      one.project.guid must be(project.guid)
      one.binaryVersion.guid must be(version.guid)

      ProjectBinaryVersionsDao.findAll(binaryVersionGuid = Some(UUID.randomUUID)) must be(Nil)
    }

  }

}

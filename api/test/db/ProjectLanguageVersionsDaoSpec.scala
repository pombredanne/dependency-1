package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ProjectLanguageVersionsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val (project, version) = createProjectWithLanguage()

  "findAll" must {

    "projectGuid" in {
      val one = ProjectLanguageVersionsDao.findAll(projectGuid = Some(project.guid)) match {
        case one :: Nil => one
        case recs => fail(s"expected 1 record but got[${recs.size}]")
      }
      one.project.guid must be(project.guid)
      one.languageVersion.guid must be(version.guid)

      ProjectLanguageVersionsDao.findAll(projectGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "languageGuid" in {
      val one = ProjectLanguageVersionsDao.findAll(languageGuid = Some(version.language.guid)) match {
        case one :: Nil => one
        case recs => fail(s"expected 1 record but got[${recs.size}]")
      }
      one.project.guid must be(project.guid)
      one.languageVersion.guid must be(version.guid)

      ProjectLanguageVersionsDao.findAll(languageGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "languageVersionGuid" in {
      val one = ProjectLanguageVersionsDao.findAll(languageVersionGuid = Some(version.guid)) match {
        case one :: Nil => one
        case recs => fail(s"expected 1 record but got[${recs.size}]")
      }
      one.project.guid must be(project.guid)
      one.languageVersion.guid must be(version.guid)

      ProjectLanguageVersionsDao.findAll(languageVersionGuid = Some(UUID.randomUUID)) must be(Nil)
    }

  }

}

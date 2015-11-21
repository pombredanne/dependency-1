package db

import org.scalatest._
import play.api.db._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class LanguageVersionsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "upsert" in {
    val language = createLanguage()
    val version1 = LanguageVersionsDao.upsert(systemUser, language.guid, "1.0")
    val version2 = LanguageVersionsDao.upsert(systemUser, language.guid, "1.0")
    val version3 = LanguageVersionsDao.upsert(systemUser, language.guid, "1.1")

    version1.guid must be(version2.guid)
    version2.guid must not be(version3.guid)
  }

  "findByGuid" in {
    val version = createLanguageVersion()
    DB.withConnection { implicit c =>
      LanguageVersionsDao.findByGuid(version.guid).map(_.guid) must be(
        Some(version.guid)
      )

      LanguageVersionsDao.findByGuid(UUID.randomUUID) must be(None)
    }
  }

  "findAll by guids" in {
    val version1 = createLanguageVersion()
    val version2 = createLanguageVersion()

    DB.withConnection { implicit c =>
      LanguageVersionsDao.findAll(guids = Some(Seq(version1.guid, version2.guid))).map(_.guid) must be(
        Seq(version1.guid, version2.guid)
      )

      LanguageVersionsDao.findAll(guids = Some(Nil)) must be(Nil)
      LanguageVersionsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
      LanguageVersionsDao.findAll(guids = Some(Seq(version1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(version1.guid))
    }
  }

  "softDelete" in {
    val language = createLanguage()
    val version1 = LanguageVersionsDao.upsert(systemUser, language.guid, "1.0")
    LanguageVersionsDao.softDelete(systemUser, version1.guid)
    val version2 = LanguageVersionsDao.upsert(systemUser, language.guid, "1.0")
    val version3 = LanguageVersionsDao.upsert(systemUser, language.guid, "1.0")

    version1.guid must not be(version2.guid)
    version2.guid must be(version3.guid)
  }

}

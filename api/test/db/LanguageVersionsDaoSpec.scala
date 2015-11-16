package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class LanguageVersionsDaoSpec @javax.inject.Inject() (
  helpers: Helpers,
  languageVersionsDao: LanguageVersionsDao
) extends PlaySpec with OneAppPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "upsert" in {
    val language = helpers.createLanguage()
    val version1 = languageVersionsDao.upsert(helpers.systemUser, language.guid, "1.0")
    val version2 = languageVersionsDao.upsert(helpers.systemUser, language.guid, "1.0")
    val version3 = languageVersionsDao.upsert(helpers.systemUser, language.guid, "1.1")

    version1.guid must be(version2.guid)
    version2.guid must not be(version3.guid)
  }

  "findByGuid" in {
    val version = helpers.createLanguageVersion()
    languageVersionsDao.findByGuid(version.guid).map(_.guid) must be(
      Some(version.guid)
    )

    languageVersionsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val version1 = helpers.createLanguageVersion()
    val version2 = helpers.createLanguageVersion()

    languageVersionsDao.findAll(guids = Some(Seq(version1.guid, version2.guid))).map(_.guid) must be(
      Seq(version1.guid, version2.guid)
    )

    languageVersionsDao.findAll(guids = Some(Nil)) must be(Nil)
    languageVersionsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    languageVersionsDao.findAll(guids = Some(Seq(version1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(version1.guid))
  }

  "softDelete" in {
    val language = helpers.createLanguage()
    val version1 = languageVersionsDao.upsert(helpers.systemUser, language.guid, "1.0")
    languageVersionsDao.softDelete(helpers.systemUser, version1.guid)
    val version2 = languageVersionsDao.upsert(helpers.systemUser, language.guid, "1.0")
    val version3 = languageVersionsDao.upsert(helpers.systemUser, language.guid, "1.0")

    version1.guid must not be(version2.guid)
    version2.guid must be(version3.guid)
  }

}

package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class LibraryVersionsDaoSpec @javax.inject.Inject() (
  helpers: Helpers,
  libraryVersionsDao: LibraryVersionsDao
) extends PlaySpec with OneAppPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "upsert" in {
    val library = helpers.createLibrary()
    val version1 = libraryVersionsDao.upsert(helpers.systemUser, library.guid, "1.0")
    val version2 = libraryVersionsDao.upsert(helpers.systemUser, library.guid, "1.0")
    val version3 = libraryVersionsDao.upsert(helpers.systemUser, library.guid, "1.1")

    version1.guid must be(version2.guid)
    version2.guid must not be(version3.guid)
  }

  "findByGuid" in {
    val version = helpers.createLibraryVersion()
    libraryVersionsDao.findByGuid(version.guid).map(_.guid) must be(
      Some(version.guid)
    )

    libraryVersionsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val version1 = helpers.createLibraryVersion()
    val version2 = helpers.createLibraryVersion()

    libraryVersionsDao.findAll(guids = Some(Seq(version1.guid, version2.guid))).map(_.guid) must be(
      Seq(version1.guid, version2.guid)
    )

    libraryVersionsDao.findAll(guids = Some(Nil)) must be(Nil)
    libraryVersionsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    libraryVersionsDao.findAll(guids = Some(Seq(version1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(version1.guid))
  }

  "softDelete" in {
    val library = helpers.createLibrary()
    val version1 = libraryVersionsDao.upsert(helpers.systemUser, library.guid, "1.0")
    libraryVersionsDao.softDelete(helpers.systemUser, version1.guid)
    val version2 = libraryVersionsDao.upsert(helpers.systemUser, library.guid, "1.0")
    val version3 = libraryVersionsDao.upsert(helpers.systemUser, library.guid, "1.0")

    version1.guid must not be(version2.guid)
    version2.guid must be(version3.guid)
  }

}

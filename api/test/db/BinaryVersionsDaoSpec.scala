package db

import org.scalatest._
import play.api.db._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class BinaryVersionsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "upsert" in {
    val binary = createBinary()
    val version1 = BinaryVersionsDao.upsert(systemUser, binary.guid, "1.0")
    val version2 = BinaryVersionsDao.upsert(systemUser, binary.guid, "1.0")
    val version3 = BinaryVersionsDao.upsert(systemUser, binary.guid, "1.1")

    version1.guid must be(version2.guid)
    version2.guid must not be(version3.guid)
  }

  "findByGuid" in {
    val version = createBinaryVersion()
    BinaryVersionsDao.findByGuid(version.guid).map(_.guid) must be(
      Some(version.guid)
    )

    BinaryVersionsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val version1 = createBinaryVersion()
    val version2 = createBinaryVersion()

    BinaryVersionsDao.findAll(guids = Some(Seq(version1.guid, version2.guid))).map(_.guid).sorted must be(
      Seq(version1.guid, version2.guid).sorted
    )

    BinaryVersionsDao.findAll(guids = Some(Nil)) must be(Nil)
    BinaryVersionsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    BinaryVersionsDao.findAll(guids = Some(Seq(version1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(version1.guid))
  }

  "softDelete" in {
    val binary = createBinary()
    val version1 = BinaryVersionsDao.upsert(systemUser, binary.guid, "1.0")
    BinaryVersionsDao.softDelete(systemUser, version1.guid)
    val version2 = BinaryVersionsDao.upsert(systemUser, binary.guid, "1.0")
    val version3 = BinaryVersionsDao.upsert(systemUser, binary.guid, "1.0")

    version1.guid must not be(version2.guid)
    version2.guid must be(version3.guid)
  }

}

package db

import com.bryzek.dependency.v0.models.SyncEvent
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class BinariesDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "findByOrganizationGuidAndName" in {
    val lang = createBinary(org)()
    BinariesDao.findByOrganizationGuidAndName(org.guid, lang.name.toString).map(_.name) must be(
      Some(lang.name)
    )

    BinariesDao.findByOrganizationGuidAndName(UUID.randomUUID, lang.name.toString) must be(None)
    BinariesDao.findByOrganizationGuidAndName(org.guid, UUID.randomUUID.toString) must be(None)
  }

  "findByGuid" in {
    val lang = createBinary(org)()
    BinariesDao.findByGuid(lang.guid).map(_.guid) must be(
      Some(lang.guid)
    )

    BinariesDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val binary1 = createBinary(org)()
    val binary2 = createBinary(org)()

    BinariesDao.findAll(guids = Some(Seq(binary1.guid, binary2.guid))).map(_.guid) must be(
      Seq(binary1, binary2).sortWith { (x,y) => x.name.toString < y.name.toString }.map(_.guid)
    )

    BinariesDao.findAll(guids = Some(Nil)) must be(Nil)
    BinariesDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    BinariesDao.findAll(guids = Some(Seq(binary1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(binary1.guid))
  }

  "findAll by isSynced" in {
    val binary = createBinary(org)()
    createSync(createSyncForm(objectGuid = binary.guid, event = SyncEvent.Completed))

    BinariesDao.findAll(guid = Some(binary.guid), isSynced = Some(true)).map(_.guid) must be(Seq(binary.guid))
    BinariesDao.findAll(guid = Some(binary.guid), isSynced = Some(false)) must be(Nil)
  }

  "create" must {
    "validates empty name" in {
      val form = createBinaryForm(org).copy(name = "   ")
      BinariesDao.validate(form) must be(
        Seq("Name cannot be empty")
      )
    }

    "validates duplicate names" in {
      val lang = createBinary(org)()
      val form = createBinaryForm(org).copy(name = lang.name.toString.toUpperCase)
      BinariesDao.validate(form) must be(
        Seq("Binary with this name already exists")
      )
    }
  }

}

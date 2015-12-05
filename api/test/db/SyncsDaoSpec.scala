package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class SyncsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "create" in {
    val form = createSyncForm()
    val sync = SyncsDao.create(systemUser, form)

    sync.event must be(form.event)
  }

  "findByGuid" in {
    val sync = createSync()
    SyncsDao.findByGuid(sync.guid).map(_.guid) must be(
      Some(sync.guid)
    )

    SyncsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val sync1 = createSync()
    val sync2 = createSync()

    SyncsDao.findAll(guids = Some(Seq(sync1.guid, sync2.guid))).map(_.guid) must be(
      Seq(sync1.guid, sync2.guid)
    )

    SyncsDao.findAll(guids = Some(Nil)) must be(Nil)
    SyncsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    SyncsDao.findAll(guids = Some(Seq(sync1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(sync1.guid))
  }

}

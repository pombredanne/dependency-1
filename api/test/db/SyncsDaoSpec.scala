package db

import com.bryzek.dependency.v0.models.SyncEvent
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

  "withStartedAndCompleted" in {
    val project = createProject()
    SyncsDao.withStartedAndCompleted(systemUser, project.guid) {
      // NO-OP
    }
    val events = SyncsDao.findAll(objectGuid = Some(project.guid)).map(_.event)
    events.contains(SyncEvent.Started) must be(true)
    events.contains(SyncEvent.Completed) must be(true)
  }

  "recordStarted" in {
    val project = createProject()
    SyncsDao.recordStarted(systemUser, project.guid)
    SyncsDao.findAll(objectGuid = Some(project.guid)).map(_.event).contains(SyncEvent.Started) must be(true)
  }

  "recordCompleted" in {
    val project = createProject()
    SyncsDao.recordCompleted(systemUser, project.guid)
    SyncsDao.findAll(objectGuid = Some(project.guid)).map(_.event).contains(SyncEvent.Completed) must be(true)
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

  "purge executes" in {
    val sync = createSync()
    SyncsDao.purgeOld()
    SyncsDao.findByGuid(sync.guid).map(_.guid) must be(Some(sync.guid))
  }

}

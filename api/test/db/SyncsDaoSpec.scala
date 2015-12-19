package db

import com.bryzek.dependency.v0.models.SyncEvent
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class SyncsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "create" in {
    val form = createSyncForm()
    val sync = SyncsDao.create(systemUser, form)

    sync.event must be(form.event)
  }

  "withStartedAndCompleted" in {
    val project = createProject(org)
    SyncsDao.withStartedAndCompleted(systemUser, "project", project.guid) {
      // NO-OP
    }
    val events = SyncsDao.findAll(objectGuid = Some(project.guid)).map(_.event)
    events.contains(SyncEvent.Started) must be(true)
    events.contains(SyncEvent.Completed) must be(true)
  }

  "recordStarted" in {
    val project = createProject(org)
    SyncsDao.recordStarted(systemUser, "project", project.guid)
    SyncsDao.findAll(objectGuid = Some(project.guid)).map(_.event).contains(SyncEvent.Started) must be(true)
  }

  "recordCompleted" in {
    val project = createProject(org)
    SyncsDao.recordCompleted(systemUser, "project", project.guid)
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

    SyncsDao.findAll(guids = Some(Seq(sync1.guid, sync2.guid))).map(_.guid).sorted must be(
      Seq(sync1.guid, sync2.guid).sorted
    )

    SyncsDao.findAll(guids = Some(Nil)) must be(Nil)
    SyncsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    SyncsDao.findAll(guids = Some(Seq(sync1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(sync1.guid))
  }

  "findAll by objectGuid and event" in {
    val start = createSync(createSyncForm(event = SyncEvent.Started))
    val completed = createSync(createSyncForm(event = SyncEvent.Completed))

    SyncsDao.findAll(
      guids = Some(Seq(start.guid, completed.guid)),
      event = Some(SyncEvent.Started)
    ).map(_.guid) must be(Seq(start.guid))

    SyncsDao.findAll(
      guids = Some(Seq(start.guid, completed.guid)),
      event = Some(SyncEvent.Completed)
    ).map(_.guid) must be(Seq(completed.guid))

    SyncsDao.findAll(
      guids = Some(Seq(start.guid, completed.guid)),
      event = Some(SyncEvent.UNDEFINED("other"))
    ) must be(Nil)
  }

  "findAll by objectGuid" in {
    val form = createSyncForm()
    val sync = createSync(form)

    SyncsDao.findAll(
      guids = Some(Seq(sync.guid)),
      objectGuid = Some(form.objectGuid)
    ).map(_.guid) must be(Seq(sync.guid))

    SyncsDao.findAll(
      objectGuid = Some(UUID.randomUUID)
    ) must be(Nil)
  }

  "purge executes" in {
    val sync = createSync()
    SyncsDao.purgeOld()
    SyncsDao.findByGuid(sync.guid).map(_.guid) must be(Some(sync.guid))
  }

}

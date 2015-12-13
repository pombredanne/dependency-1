package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class WatchProjectsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "upsert" in {
    val form = createWatchProjectForm(org)()
    val watchProject1 = WatchProjectsDao.create(systemUser, form).right.get

    val watchProject2 = WatchProjectsDao.upsert(systemUser, form)
    watchProject1.guid must be(watchProject2.guid)

    val newWatchProject = UUID.randomUUID.toString
    val watchProject3 = createWatchProject(org)()

    watchProject2.guid must not be(watchProject3.guid)
  }

  "findByGuid" in {
    val watchProject = createWatchProject(org)()
    WatchProjectsDao.findByGuid(watchProject.guid).map(_.guid) must be(
      Some(watchProject.guid)
    )

    WatchProjectsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findByWatchProjectGuidAndProjectGuid" in {
    val watchProject = createWatchProject(org)()
    WatchProjectsDao.findByUserGuidAndProjectGuid(watchProject.user.guid, watchProject.project.guid).map(_.guid) must be(
      Some(watchProject.guid)
    )

    WatchProjectsDao.findByUserGuidAndProjectGuid(UUID.randomUUID, watchProject.project.guid).map(_.guid) must be(None)
    WatchProjectsDao.findByUserGuidAndProjectGuid(watchProject.user.guid, UUID.randomUUID).map(_.guid) must be(None)
  }

  "findAll by guids" in {
    val watchProject1 = createWatchProject(org)()
    val watchProject2 = createWatchProject(org)()

    WatchProjectsDao.findAll(guids = Some(Seq(watchProject1.guid, watchProject2.guid))).map(_.guid) must be(
      Seq(watchProject1.guid, watchProject2.guid)
    )

    WatchProjectsDao.findAll(guids = Some(Nil)) must be(Nil)
    WatchProjectsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    WatchProjectsDao.findAll(guids = Some(Seq(watchProject1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(watchProject1.guid))
  }

}

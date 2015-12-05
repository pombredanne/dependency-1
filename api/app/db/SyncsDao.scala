package db

import com.bryzek.dependency.v0.models.{Sync, SyncEvent}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Filters}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import scala.util.{Failure, Success, Try}

case class SyncForm(
  objectGuid: UUID,
  event: SyncEvent
)

object SyncsDao {

  private[this] val BaseQuery = s"""
    select syncs.guid,
           syncs.object_guid,
           syncs.event,
           ${AuditsDao.creationOnly("syncs")}
      from syncs
     where true
  """

  private[this] val InsertQuery = """
    insert into syncs
    (guid, object_guid, event, created_by_guid)
    values
    ({guid}::uuid, {object_guid}::uuid, {event}, {created_by_guid}::uuid)
  """

  private[this] val PurgeQuery = """
    delete from syncs where created_at < now() - interval '7 days'
  """

  def withStartedAndCompleted[T](
    createdBy: User, guid: UUID
  ) (
    f: => T
  ): T = {
    recordStarted(createdBy, guid)
    val result = f
    recordCompleted(createdBy, guid)
    result
  }

  def recordStarted(createdBy: User, guid: UUID) {
    createInternal(createdBy, SyncForm(guid, SyncEvent.Started))
  }

  def recordCompleted(createdBy: User, guid: UUID) {
    createInternal(createdBy, SyncForm(guid, SyncEvent.Completed))
  }

  def create(createdBy: User, form: SyncForm): Sync = {
    val guid = createInternal(createdBy, form)
    findByGuid(guid).getOrElse {
      sys.error("Failed to create sync")
    }

  }

  private[this] def createInternal(createdBy: User, form: SyncForm): UUID = {
    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'object_guid -> form.objectGuid,
        'event -> form.event.toString,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    guid
  }

  def purgeOld() {
    DB.withConnection { implicit c =>
      SQL(PurgeQuery).execute()
    }
  }

  def findByGuid(guid: UUID): Option[Sync] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    objectGuid: Option[UUID] = None,
    event: Option[SyncEvent] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Sync] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v =>  "and syncs.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("syncs.guid", _) },
      objectGuid.map { v => "and syncs.object_guid = {object_guid}::uuid" },
      event.map { v => "and syncs.event = {event}" }
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      objectGuid.map('object_guid -> _.toString),
      event.map('event -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Sync.table("syncs").*
      )
    }
  }

}

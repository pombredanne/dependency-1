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
  `type`: String,
  objectGuid: UUID,
  event: SyncEvent
)

object SyncsDao {

  private[this] val BaseQuery = Query(s"""
    select syncs.guid,
           syncs.type,
           syncs.object_guid,
           syncs.event,
           ${AuditsDao.creationOnly("syncs")}
      from syncs
  """)

  private[this] val InsertQuery = """
    insert into syncs
    (guid, type, object_guid, event, created_by_guid)
    values
    ({guid}::uuid, {type}, {object_guid}::uuid, {event}, {created_by_guid}::uuid)
  """

  private[this] val PurgeQuery = """
    delete from syncs where created_at < now() - interval '7 days'
  """

  def withStartedAndCompleted[T](
    createdBy: User, `type`: String, guid: UUID
  ) (
    f: => T
  ): T = {
    recordStarted(createdBy, `type`, guid)
    val result = f
    recordCompleted(createdBy, `type`, guid)
    result
  }

  def recordStarted(createdBy: User, `type`: String, guid: UUID) {
    createInternal(createdBy, SyncForm(`type`, guid, SyncEvent.Started))
  }

  def recordCompleted(createdBy: User, `type`: String, guid: UUID) {
    createInternal(createdBy, SyncForm(`type`, guid, SyncEvent.Completed))
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
        'type -> form.`type`,
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
    orderBy: OrderBy = OrderBy.parseOrError("-syncs.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Sync] = {
    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "syncs",
        auth = Clause.True, // TODO
        guid = guid,
        guids = guids,
        orderBy = orderBy,
        isDeleted = None,
        limit = limit,
        offset = offset
      ).
        uuid("syncs.object_guid", objectGuid).
        text("syncs.event", event).
        as(
          com.bryzek.dependency.v0.anorm.parsers.Sync.table("syncs").*
        )
    }
  }

}

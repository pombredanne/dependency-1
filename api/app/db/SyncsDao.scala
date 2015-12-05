package db

import com.bryzek.dependency.v0.models.{Sync, SyncEvent}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
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
           ${AuditsDao.all("syncs")}
      from syncs
     where true
  """

  private[this] val InsertQuery = """
    insert into syncs
    (guid, object_guid, event, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {object_guid}::uuid, {event}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def create(createdBy: User, form: SyncForm): Sync = {
    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'object_guid -> form.objectGuid,
        'event -> form.event.toString,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create sync")
    }
  }

  def softDelete(deletedBy: User, sync: Sync) {
    SoftDelete.delete("syncs", deletedBy.guid, sync.guid)
  }

  def findByGuid(guid: UUID): Option[Sync] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    objectGuid: Option[UUID] = None,
    event: Option[SyncEvent] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Sync] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v =>  "and syncs.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("syncs.guid", _) },
      objectGuid.map { v => "and syncs.object_guid = {object_guid}::uuid" },
      event.map { v => "and syncs.event = {event}" },
      isDeleted.map(Filters.isDeleted("syncs", _))
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

package db

import com.bryzek.dependency.v0.models.{BinarySummary, Item, ItemDetail, ItemDetailUndefinedType, LibrarySummary, Project, ProjectSummary}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class ItemForm(
  detail: ItemDetail,
  label: String,
  description: Option[String]
)

object ItemsDao {

  private[this] val BaseQuery = s"""
    select items.guid,
           items.object_guid,
           items.label,
           items.description,
           items.metadata,
           items.created_at,
           items.deleted_at,
           items.object_guid as items_detail_guid,
           items.metadata->>'name' items_detail_name,
           items.metadata->>'group_id' items_detail_group_id,
           items.metadata->>'artifact_id' items_detail_artifact_id
      from items
     where true
  """

  private[this] val InsertQuery = """
    insert into items
    (guid, object_guid, label, description, metadata, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {object_guid}::uuid, {label}, {description}, {metadata}::json, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] def objectGuid(detail: ItemDetail): UUID = {
    detail match {
      case BinarySummary(guid, _) => guid
      case LibrarySummary(guid, _, _) => guid
      case ProjectSummary(guid, _) => guid
      case ItemDetailUndefinedType(name) => sys.error(s"Cannot get a guid from type[$name]")
    }
  }

  private[this] def metadata(detail: ItemDetail): Option[Map[String, String]] = {
    detail match {
      case BinarySummary(_, binaryType) => Some(
        Map("name" -> binaryType.toString)
      )
      case LibrarySummary(_, groupId, artifactId) => Some(
        Map(
          "group_id" -> groupId,
          "artifact_id" -> artifactId
        )
      )
      case ProjectSummary(_, name) => Some(
        Map("name" -> name)
      )
      case ItemDetailUndefinedType(name) => None
    }
  }

  def upsert(user: User, form: ItemForm): Item = {
    findByObjectGuid(objectGuid(form.detail)) match {
      case Some(item) => item
      case None => create(user, form)
    }
  }

  def create(createdBy: User, form: ItemForm): Item = {
    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'object_guid -> objectGuid(form.detail),
        'label -> form.label,
        'description -> form.description,
        'metadata -> metadata(form.detail).map { data => Json.stringify(Json.toJson(data)) },
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create item")
    }
  }

  def findByGuid(guid: UUID): Option[Item] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findByObjectGuid(objectGuid: UUID): Option[Item] = {
    findAll(objectGuid = Some(objectGuid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    objectGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Item] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v =>  "and items.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("items.guid", _) },
      objectGuid.map { v => "and items.object_guid = {object_guid}::uuid" },
      isDeleted.map(Filters.isDeleted("items", _)),
      (limit >= 0) match {
        case true => Some(s"order by lower(items.label), items.created_at limit ${limit} offset ${offset}")
        case false => None
      }
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      objectGuid.map('object_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Item.table("items").*
      )
    }
  }

}

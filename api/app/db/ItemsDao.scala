package db

import com.bryzek.dependency.v0.models.{Binary, BinarySummary, Item, ItemSummary, ItemSummaryUndefinedType, Library, LibrarySummary, Project, ProjectSummary}
import com.bryzek.dependency.v0.models.json._
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import scala.util.{Failure, Success, Try}

case class ItemForm(
  detail: ItemSummary,
  label: String,
  description: Option[String]
)

object ItemsDao {

  private[this] val BaseQuery = s"""
    select items.guid,
           items.object_guid,
           items.label,
           items.description,
           items.detail,
           items.created_at,
           items.deleted_at
      from items
     where true
  """

  private[this] val InsertQuery = """
    insert into items
    (guid, object_guid, label, description, detail, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {object_guid}::uuid, {label}, {description}, {detail}::json, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] def objectGuid(detail: ItemSummary): UUID = {
    detail match {
      case BinarySummary(guid, _) => guid
      case LibrarySummary(guid, _, _) => guid
      case ProjectSummary(guid, _) => guid
      case ItemSummaryUndefinedType(name) => sys.error(s"Cannot get a guid from ItemSummaryUndefinedType($name)")
    }
  }

  def upsertBinary(user: User, binary: Binary): Item = {
    upsert(
      user,
      ItemForm(
        detail = BinarySummary(
          guid = binary.guid,
          name = binary.name
        ),
        label = binary.name.toString,
        description = None
      )
    )
  }

  def upsertLibrary(user: User, library: Library): Item = {
    upsert(
      user,
      ItemForm(
        detail = LibrarySummary(
          guid = library.guid,
          groupId = library.groupId,
          artifactId = library.artifactId
        ),
        label = Seq(library.groupId, library.artifactId).mkString("."),
        description = None
      )
    )
  }

  def upsertProject(user: User, project: Project): Item = {
    upsert(
      user,
      ItemForm(
        detail = ProjectSummary(
          guid = project.guid,
          name = project.name
        ),
        label = project.name,
        description = Some(project.uri)
      )
    )
  }

  def upsert(user: User, form: ItemForm): Item = {
    findByObjectGuid(objectGuid(form.detail)) match {
      case Some(item) => item
      case None => {
        Try(create(user, form)) match {
          case Success(item) => item
          case Failure(ex) => {
            findByObjectGuid(objectGuid(form.detail)).getOrElse {
              sys.error(s"Failed to upsert item: $ex")
            }
          }
        }
      }
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
        'detail -> Json.stringify(Json.toJson(form.detail)),
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create item")
    }
  }

  def softDelete(deletedBy: User, item: Item) {
    SoftDelete.delete("items", deletedBy.guid, item.guid)
  }

  def softDeleteByObjectGuid(deletedBy: User, objectGuid: UUID) {
    findByObjectGuid(objectGuid).map { item =>
      softDelete(deletedBy, item)
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

package db

import com.bryzek.dependency.v0.models.{Binary, BinarySummary, Item, ItemSummary, ItemSummaryUndefinedType, Library, LibrarySummary}
import com.bryzek.dependency.v0.models.{OrganizationSummary, Project, ProjectSummary, ResolverSummary, Visibility}
import com.bryzek.dependency.v0.models.json._
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Query, OrderBy, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import scala.util.{Failure, Success, Try}

case class ItemForm(
  summary: ItemSummary,
  label: String,
  description: Option[String],
  contents: String
)

object ItemsDao {

  private[this] val BaseQuery = Query(s"""
    select items.guid,
           items.organization_guid,
           items.visibility,
           items.object_guid,
           items.label,
           items.description,
           items.contents,
           items.summary,
           items.created_at,
           items.deleted_at,
           organizations.guid as items_organization_guid,
           organizations.key as items_organization_key
      from items
      join organizations on organizations.deleted_at is null and organizations.guid = items.organization_guid
  """)

  private[this] val InsertQuery = """
    insert into items
    (guid, organization_guid, visibility, object_guid, label, description, contents, summary, created_by_guid, updated_by_guid)
    values
    ({guid}::equals, {organization_guid}::equals, {visibility}, {object_guid}::equals, {label}, {description}, {contents}, {summary}::json, {created_by_guid}::equals, {created_by_guid}::equals)
  """

  private[this] def objectGuid(summary: ItemSummary): UUID = {
    summary match {
      case BinarySummary(guid, org, name) => guid
      case LibrarySummary(guid, org, groupId, artifactId) => guid
      case ProjectSummary(guid, org, name) => guid
      case ItemSummaryUndefinedType(name) => sys.error(s"Cannot get a guid from ItemSummaryUndefinedType($name)")
    }
  }

  private[this] def organization(summary: ItemSummary): OrganizationSummary = {
    summary match {
      case BinarySummary(guid, org, name) => org
      case LibrarySummary(guid, org, groupId, artifactId) => org
      case ProjectSummary(guid, org, name) => org
      case ItemSummaryUndefinedType(name) => sys.error(s"Cannot get a guid from ItemSummaryUndefinedType($name)")
    }
  }

  private[this] def visibility(summary: ItemSummary): Visibility = {
    summary match {
      case BinarySummary(guid, org, name) => {
        Visibility.Public
      }
      case LibrarySummary(guid, org, groupId, artifactId) => {
        LibrariesDao.findByGuid(Authorization.All, guid).map(_.resolver.visibility).getOrElse(Visibility.Private)
      }
      case ProjectSummary(guid, org, name) => {
        ProjectsDao.findByGuid(Authorization.All, guid).map(_.visibility).getOrElse(Visibility.Private)
      }
      case ItemSummaryUndefinedType(name) => {
        Visibility.Private
      }
    }
  }

  private[this] def visibility(resolver: ResolverSummary): Visibility = {
    ResolversDao.findByGuid(Authorization.All, resolver.guid).map(_.visibility).getOrElse(Visibility.Private)
  }

  def upsertBinary(user: User, binary: Binary): Item = {
    val label = binary.name.toString
    upsert(
      user,
      ItemForm(
        summary = BinarySummary(
          guid = binary.guid,
          organization = binary.organization,
          name = binary.name
        ),
        label = label,
        description = None,
        contents = Seq(binary.guid.toString, label).mkString(" ")
      )
    )
  }

  def upsertLibrary(user: User, library: Library): Item = {
    val label = Seq(library.groupId, library.artifactId).mkString(".")
    upsert(
      user,
      ItemForm(
        summary = LibrarySummary(
          guid = library.guid,
          organization = library.organization,
          groupId = library.groupId,
          artifactId = library.artifactId
        ),
        label = label,
        description = None,
        contents = Seq(library.guid.toString, label).mkString(" ")
      )
    )
  }

  def upsertProject(user: User, project: Project): Item = {
    val label = project.name
    val description = project.uri

    upsert(
      user,
      ItemForm(
        summary = ProjectSummary(
          guid = project.guid,
          organization = project.organization,
          name = project.name
        ),
        label = label,
        description = Some(description),
        contents = Seq(project.guid.toString, label, description).mkString(" ")
      )
    )
  }

  def upsert(user: User, form: ItemForm): Item = {
    findByObjectGuid(Authorization.All, objectGuid(form.summary)) match {
      case Some(item) => item
      case None => {
        Try(create(user, form)) match {
          case Success(item) => item
          case Failure(ex) => {
            findByObjectGuid(Authorization.All, objectGuid(form.summary)).getOrElse {
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
        'organization_guid -> organization(form.summary).guid,
        'visibility -> visibility(form.summary).toString,
        'object_guid -> objectGuid(form.summary),
        'label -> form.label,
        'description -> form.description,
        'contents -> form.contents.trim.toLowerCase,
        'summary -> Json.stringify(Json.toJson(form.summary)),
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    findByGuid(Authorization.All, guid).getOrElse {
      sys.error("Failed to create item")
    }
  }

  def softDelete(deletedBy: User, item: Item) {
    SoftDelete.delete("items", deletedBy.guid, item.guid)
  }

  def softDeleteByObjectGuid(auth: Authorization, deletedBy: User, objectGuid: UUID) {
    findByObjectGuid(auth, objectGuid).map { item =>
      softDelete(deletedBy, item)
    }
  }

  def findByGuid(auth: Authorization, guid: UUID): Option[Item] = {
    findAll(auth, guid = Some(guid), limit = Some(1)).headOption
  }

  def findByObjectGuid(auth: Authorization, objectGuid: UUID): Option[Item] = {
    findAll(auth, objectGuid = Some(objectGuid), limit = Some(1)).headOption
  }

  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    q: Option[String] = None,
    objectGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("-lower(items.label), items.created_at"),
    limit: Option[Long] = Some(25),
    offset: Long = 0
  ): Seq[Item] = {
    DB.withConnection { implicit c =>
      BaseQuery.
        condition(Some(auth.organizations("items.organization_guid", Some("items.visibility")).sql)).
        equals("items.guid", guid).
        in("items.guid", guids).
        condition(q.map { v => "items.contents like '%' || lower(trim({q})) || '%' " }).
        bind("q", q).
        equals("items.object_guid", objectGuid).
        nullBoolean("items.deleted_at", isDeleted).
        orderBy(orderBy.sql).
        limit(limit).
        offset(Some(offset)).
        as(
          com.bryzek.dependency.v0.anorm.parsers.Item.table("items").*
        )
    }
  }

}

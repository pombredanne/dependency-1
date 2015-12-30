package db

import com.bryzek.dependency.v0.models.{Binary, BinarySummary, Item, ItemSummary, ItemSummaryUndefinedType, Library, LibrarySummary}
import com.bryzek.dependency.v0.models.{OrganizationSummary, Project, ProjectSummary, ResolverSummary, Visibility}
import com.bryzek.dependency.v0.models.json._
import io.flow.user.v0.models.User
import io.flow.postgresql.{Query, OrderBy}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import scala.util.{Failure, Success, Try}

case class ItemForm(
  summary: ItemSummary,
  label: String,
  description: Option[String],
  contents: String
)

object ItemsDao {

  private[this] val BaseQuery = Query(s"""
    select items.id,
           items.organization_id,
           items.visibility,
           items.object_id,
           items.label,
           items.description,
           items.contents,
           items.summary,
           items.created_at,
           items.deleted_at,
           organizations.id as items_organization_id,
           organizations.key as items_organization_key
      from items
      join organizations on organizations.deleted_at is null and organizations.id = items.organization_id
  """)

  private[this] val InsertQuery = """
    insert into items
    (id, organization_id, visibility, object_id, label, description, contents, summary, updated_by_user_id)
    values
    ({id}, {organization_id}, {visibility}, {object_id}, {label}, {description}, {contents}, {summary}::json, {updated_by_user_id})
  """

  private[this] def objectId(summary: ItemSummary): String = {
    summary match {
      case BinarySummary(id, org, name) => id
      case LibrarySummary(id, org, groupId, artifactId) => id
      case ProjectSummary(id, org, name) => id
      case ItemSummaryUndefinedType(name) => sys.error(s"Cannot get a id from ItemSummaryUndefinedType($name)")
    }
  }

  private[this] def organization(summary: ItemSummary): OrganizationSummary = {
    summary match {
      case BinarySummary(id, org, name) => org
      case LibrarySummary(id, org, groupId, artifactId) => org
      case ProjectSummary(id, org, name) => org
      case ItemSummaryUndefinedType(name) => sys.error(s"Cannot get a id from ItemSummaryUndefinedType($name)")
    }
  }

  private[this] def visibility(summary: ItemSummary): Visibility = {
    summary match {
      case BinarySummary(id, org, name) => {
        Visibility.Public
      }
      case LibrarySummary(id, org, groupId, artifactId) => {
        LibrariesDao.findById(Authorization.All, id).map(_.resolver.visibility).getOrElse(Visibility.Private)
      }
      case ProjectSummary(id, org, name) => {
        ProjectsDao.findById(Authorization.All, id).map(_.visibility).getOrElse(Visibility.Private)
      }
      case ItemSummaryUndefinedType(name) => {
        Visibility.Private
      }
    }
  }

  private[this] def visibility(resolver: ResolverSummary): Visibility = {
    ResolversDao.findById(Authorization.All, resolver.id).map(_.visibility).getOrElse(Visibility.Private)
  }

  def upsertBinary(user: User, binary: Binary): Item = {
    val label = binary.name.toString
    upsert(
      user,
      ItemForm(
        summary = BinarySummary(
          id = binary.id,
          organization = binary.organization,
          name = binary.name
        ),
        label = label,
        description = None,
        contents = Seq(binary.id.toString, label).mkString(" ")
      )
    )
  }

  def upsertLibrary(user: User, library: Library): Item = {
    val label = Seq(library.groupId, library.artifactId).mkString(".")
    upsert(
      user,
      ItemForm(
        summary = LibrarySummary(
          id = library.id,
          organization = library.organization,
          groupId = library.groupId,
          artifactId = library.artifactId
        ),
        label = label,
        description = None,
        contents = Seq(library.id.toString, label).mkString(" ")
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
          id = project.id,
          organization = project.organization,
          name = project.name
        ),
        label = label,
        description = Some(description),
        contents = Seq(project.id.toString, label, description).mkString(" ")
      )
    )
  }

  def upsert(user: User, form: ItemForm): Item = {
    findByObjectId(Authorization.All, objectId(form.summary)) match {
      case Some(item) => item
      case None => {
        Try(create(user, form)) match {
          case Success(item) => item
          case Failure(ex) => {
            findByObjectId(Authorization.All, objectId(form.summary)).getOrElse {
              sys.error(s"Failed to upsert item: $ex")
            }
          }
        }
      }
    }
  }

  def create(createdBy: User, form: ItemForm): Item = {
    val id = io.flow.play.util.IdGenerator("itm").randomId()

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'id -> id,
        'organization_id -> organization(form.summary).id,
        'visibility -> visibility(form.summary).toString,
        'object_id -> objectId(form.summary),
        'label -> form.label,
        'description -> form.description,
        'contents -> form.contents.trim.toLowerCase,
        'summary -> Json.stringify(Json.toJson(form.summary)),
        'updated_by_user_id -> createdBy.id
      ).execute()
    }

    findById(Authorization.All, id).getOrElse {
      sys.error("Failed to create item")
    }
  }

  def softDelete(deletedBy: User, item: Item) {
    SoftDelete.delete("items", deletedBy.id, item.id)
  }

  def softDeleteByObjectId(auth: Authorization, deletedBy: User, objectId: String) {
    findByObjectId(auth, objectId).map { item =>
      softDelete(deletedBy, item)
    }
  }

  def findById(auth: Authorization, id: String): Option[Item] = {
    findAll(auth, id = Some(id), limit = Some(1)).headOption
  }

  def findByObjectId(auth: Authorization, objectId: String): Option[Item] = {
    findAll(auth, objectId = Some(objectId), limit = Some(1)).headOption
  }

  def findAll(
    auth: Authorization,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    q: Option[String] = None,
    objectId: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("-lower(items.label), items.created_at"),
    limit: Option[Long] = Some(25),
    offset: Long = 0
  ): Seq[Item] = {
    DB.withConnection { implicit c =>
      BaseQuery.
        condition(Some(auth.organizations("items.organization_id", Some("items.visibility")).sql)).
        equals("items.id", id).
        in("items.id", ids).
        condition(q.map { v => "items.contents like '%' || lower(trim({q})) || '%' " }).
        bind("q", q).
        equals("items.object_id", objectId).
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

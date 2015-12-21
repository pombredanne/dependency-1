package db

import com.bryzek.dependency.v0.models.{Project, Recommendation, RecommendationType}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Query, OrderBy, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object RecommendationsDao {

  private[this] case class RecommendationForm(
    projectGuid: UUID,
    `type`: RecommendationType,
    objectGuid: UUID,
    name: String,
    from: String,
    to: String
  )

  private[this] val BaseQuery = Query(s"""
    select recommendations.guid,
           recommendations.type,
           recommendations.object_guid as recommendations_object_guid,
           recommendations.name,
           recommendations.from_version as "recommendations.from",
           recommendations.to_version as "recommendations.to",
           ${AuditsDao.all("recommendations")},
           projects.guid as recommendations_project_guid,
           projects.name as recommendations_project_name,
           organizations.guid as recommendations_project_organization_guid,
           organizations.key as recommendations_project_organization_key
      from recommendations
      join projects on
             projects.deleted_at is null and
             projects.guid = recommendations.project_guid
      join organizations on
             organizations.deleted_at is null and
             organizations.guid = projects.organization_guid
  """)

  private[this] val InsertQuery = """
    insert into recommendations
    (guid, project_guid, type, object_guid, name, from_version, to_version, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {project_guid}::uuid, {type}, {object_guid}::uuid, {name}, {from_version}, {to_version}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def sync(user: User, project: Project) {
    val libraries = LibraryRecommendationsDao.forProject(project).map { rec =>
      RecommendationForm(
        projectGuid = project.guid,
        `type` = RecommendationType.Library,
        objectGuid = rec.library.guid,
        name = Seq(rec.library.groupId, rec.library.artifactId).mkString("."),
        from = rec.from,
        to = rec.to.version
      )
    }

    val binaries = BinaryRecommendationsDao.forProject(project).map { rec =>
      RecommendationForm(
        projectGuid = project.guid,
        `type` = RecommendationType.Binary,
        objectGuid = rec.binary.guid,
        name = rec.binary.name.toString,
        from = rec.from,
        to = rec.to.version
      )
    }

    val newRecords = libraries ++ binaries

    val existing = RecommendationsDao.findAll(projectGuid = Some(project.guid), limit = None)

    val toAdd = newRecords.filter { rec => !existing.map(toForm(_)).contains(rec) }
    val toRemove = existing.filter { rec => !newRecords.contains(toForm(rec)) }

    DB.withTransaction { implicit c =>
      toAdd.foreach { create(user, _) }
      toRemove.foreach { rec =>
        SoftDelete.delete(c, "recommendations", user.guid, rec.guid)
      }
    }

    if (!toAdd.isEmpty) {
      // TODO: raise event that we found stuff for this project to
      // enable things like notifications.
    }
  }

  def softDelete(deletedBy: User, rec: Recommendation) {
    SoftDelete.delete("recommendations", deletedBy.guid, rec.guid)
  }

  private[this] def create(
    createdBy: User,
    form: RecommendationForm
  ) (
    implicit c: java.sql.Connection
  ) {
    val guid = UUID.randomUUID
    SQL(InsertQuery).on(
      'guid -> guid,
      'project_guid -> form.projectGuid,
      'type -> form.`type`.toString,
      'object_guid -> form.objectGuid,
      'name -> form.name,
      'from_version -> form.from,
      'to_version -> form.to,
      'created_by_guid -> createdBy.guid
    ).execute()
  }

  def findByGuid(guid: UUID): Option[Recommendation] = {
    findAll(guid = Some(guid), limit = Some(1)).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    `type`: Option[RecommendationType] = None,
    objectGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy.parseOrError("-recommendations.created_at, lower(projects.name), lower(recommendations.name)"),
    limit: Option[Long] = Some(25),
    offset: Long = 0
  ): Seq[Recommendation] = {
    val auth = Authorization.All

    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "recommendations",
        auth = auth.organizations("projects.organization_guid", Some("projects.visibility")),
        guid = guid,
        guids = guids,
        orderBy = orderBy.sql,
        isDeleted = isDeleted,
        limit = limit,
        offset = offset
      ).
        subquery("recommendations.project_guid", "user_guid", userGuid, { bind =>
          s"select project_guid from watch_projects where deleted_at is null and user_guid = {$bind}::uuid"
        }).
        uuid("recommendations.project_guid", projectGuid).
        text(
          "recommendations.type",
          `type`,
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        uuid("recommendations.object_guid", objectGuid).
        as(
          com.bryzek.dependency.v0.anorm.parsers.Recommendation.table("recommendations").*
        )
    }
  }

  private[this] def toForm(rec: Recommendation): RecommendationForm = RecommendationForm(
    projectGuid = rec.project.guid,
    `type` = rec.`type`,
    objectGuid = rec.`object`.guid,
    name = rec.name,
    from = rec.from,
    to = rec.to
  )

}

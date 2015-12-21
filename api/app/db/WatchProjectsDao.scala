package db

import com.bryzek.dependency.v0.models.{WatchProject, WatchProjectForm}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Query, OrderBy, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object WatchProjectsDao {

  private[this] val BaseQuery = Query(s"""
    select watch_projects.guid,
           watch_projects.user_guid as watch_projects_user_guid,
           ${AuditsDao.all("watch_projects")},
           projects.guid as watch_projects_project_guid,
           projects.visibility as watch_projects_project_visibility,
           projects.scms as watch_projects_project_scms,
           projects.name as watch_projects_project_name,
           projects.uri as watch_projects_project_uri,
           ${AuditsDao.all("projects", Some("watch_projects_project"))},
           organizations.guid as watch_projects_project_organization_guid,
           organizations.key as watch_projects_project_organization_key
      from watch_projects
      join projects on projects.deleted_at is null and projects.guid = watch_projects.project_guid
      join organizations on organizations.deleted_at is null and organizations.guid = projects.organization_guid
  """)

  private[this] val InsertQuery = """
    insert into watch_projects
    (guid, user_guid, project_guid, updated_by_guid, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {project_guid}::uuid, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[db] def validate(
    user: User,
    form: WatchProjectForm
  ): Seq[String] = {
    val userErrors = UsersDao.findByGuid(form.userGuid) match {
      case None => Seq("User not found")
      case Some(_) => Nil
    }

    val projectErrors = ProjectsDao.findByGuid(Authorization.User(user.guid), form.projectGuid) match {
      case None => {
        ProjectsDao.findByGuid(Authorization.All, form.projectGuid) match {
          case None => Seq("Project not found")
          case Some(_) => Seq("Not authorized to access this project")
        }
      }
      case Some(_) => Nil
    }

    userErrors ++ projectErrors
  }

  def upsert(createdBy: User, form: WatchProjectForm): WatchProject = {
    findByUserGuidAndProjectGuid(form.userGuid, form.projectGuid).getOrElse {
      create(createdBy, form) match {
        case Left(errors) => sys.error(errors.mkString(", "))
        case Right(watch) => watch
      }
    }
  }

  def create(createdBy: User, form: WatchProjectForm): Either[Seq[String], WatchProject] = {
    validate(createdBy, form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'user_guid -> form.userGuid,
            'project_guid -> form.projectGuid,
            'created_by_guid -> createdBy.guid
          ).execute()
        }

        Right(
          findByGuid(guid).getOrElse {
            sys.error("Failed to create watch project")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, watch: WatchProject) {
    SoftDelete.delete("watch_projects", deletedBy.guid, watch.guid)
  }

  def findByUserGuidAndProjectGuid(
    userGuid: UUID,
    projectGuid: UUID
  ): Option[WatchProject] = {
    findAll(
      userGuid = Some(userGuid),
      projectGuid = Some(projectGuid),
      limit = 1
    ).headOption
  }

  def findByGuid(guid: UUID): Option[WatchProject] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy.parseOrError("watch_projects.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[WatchProject] = {
    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "watch_projects",
        auth = Clause.True, // TODO
        guid = guid,
        guids = guids,
        orderBy = orderBy.sql,
        isDeleted = isDeleted,
        limit = Some(limit),
        offset = offset
      ).
        uuid("watch_projects.user_guid", userGuid).
        uuid("watch_projects.project_guid", projectGuid).
        as(
          com.bryzek.dependency.v0.anorm.parsers.WatchProject.table("watch_projects").*
        )
    }
  }

}

package db

import com.bryzek.dependency.v0.models.{WatchProject, WatchProjectForm}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object WatchProjectsDao {

  private[this] val BaseQuery = s"""
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
      left join organizations on organizations.deleted_at is null and organizations.guid = projects.organization_guid
     where true
  """

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
    limit: Long = 25,
    offset: Long = 0
  ): Seq[WatchProject] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v =>  "and watch_projects.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("watch_projects.guid", _) },
      userGuid.map { v => "and watch_projects.user_guid = {user_guid}::uuid" },
      projectGuid.map { v => "and watch_projects.project_guid = {project_guid}::uuid" },
      isDeleted.map(Filters.isDeleted("watch_projects", _)),
      Some(s"order by watch_projects.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      projectGuid.map('project_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.WatchProject.table("watch_projects").*
      )
    }
  }

}

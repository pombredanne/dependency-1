package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.api.lib.Version
import com.bryzek.dependency.v0.models.{Binary, BinaryType, ProjectBinary, SyncEvent}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class ProjectBinaryForm(
  projectGuid: UUID,
  name: BinaryType,
  version: String,
  path: String
)

object ProjectBinariesDao {

  private[this] val BaseQuery = s"""
    select project_binaries.guid,
           project_binaries.name,
           project_binaries.version,
           project_binaries.path,
           project_binaries.binary_guid as project_binaries_binary_guid,
           ${AuditsDao.all("project_binaries")},
           projects.guid as project_binaries_project_guid,
           projects.name as project_binaries_project_name,
           organizations.guid as project_binaries_project_organization_guid,
           organizations.key as project_binaries_project_organization_key
      from project_binaries
      join projects on projects.deleted_at is null and projects.guid = project_binaries.project_guid
      join organizations on organizations.deleted_at is null and organizations.guid = projects.organization_guid
     where true
  """

  private[this] val InsertQuery = """
    insert into project_binaries
    (guid, project_guid, name, version, path, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {project_guid}::uuid, {name}, {version}, {path}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val SetBinaryQuery = """
    update project_binaries
       set binary_guid = {binary_guid}::uuid,
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  private[db] def validate(
    user: User,
    form: ProjectBinaryForm
  ): Seq[String] = {
    val nameErrors = if (form.name.toString.trim.isEmpty) {
      Seq("Name cannot be empty")
    } else {
      Nil
    }

    val versionErrors = if (form.version.trim.isEmpty) {
      Seq("Version cannot be empty")
    } else {
      Nil
    }

    val projectErrors = ProjectsDao.findByGuid(Authorization.All, form.projectGuid) match {
      case None => Seq("Project not found")
      case Some(project) => {
        MembershipsDao.isMember(project.organization.guid, user) match {
          case false => Seq("You are not authorized to edit this project")
          case true => Nil
        }
      }
    }

    val existsErrors = if (nameErrors.isEmpty && versionErrors.isEmpty) {
      ProjectBinariesDao.findByProjectGuidAndNameAndVersion(
        Authorization.All, form.projectGuid, form.name.toString, form.version
      ) match {
        case None => Nil
        case Some(lib) => {
          Seq("Project binary with this name and version already exists")
        }
      }
    } else {
      Nil
    }

    projectErrors ++ nameErrors ++ versionErrors ++ existsErrors
  }

  def upsert(createdBy: User, form: ProjectBinaryForm): Either[Seq[String], ProjectBinary] = {
    ProjectBinariesDao.findByProjectGuidAndNameAndVersion(
      Authorization.All, form.projectGuid, form.name.toString, form.version
    ) match {
      case None => {
        create(createdBy, form)
      }
      case Some(lib) => {
        Right(lib)
      }
    }
  }

  def create(createdBy: User, form: ProjectBinaryForm): Either[Seq[String], ProjectBinary] = {
    validate(createdBy, form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'project_guid -> form.projectGuid,
            'name -> form.name.toString.trim,
            'version -> form.version.trim,
            'path -> form.path.trim,
            'created_by_guid -> createdBy.guid
          ).execute()
          MainActor.ref ! MainActor.Messages.ProjectBinaryCreated(form.projectGuid, guid)
        }

        Right(
          findByGuid(Authorization.All, guid).getOrElse {
            sys.error("Failed to create project binary")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def setBinary(user: User, projectBinary: ProjectBinary, binary: Binary) {
    DB.withConnection { implicit c =>
      SQL(SetBinaryQuery).on(
        'guid -> projectBinary.guid,
        'binary_guid -> binary.guid,
        'updated_by_guid -> user.guid
      ).execute()
    }
  }

  def softDelete(deletedBy: User, binary: ProjectBinary) {
    SoftDelete.delete("project_binaries", deletedBy.guid, binary.guid)
    MainActor.ref ! MainActor.Messages.ProjectBinaryDeleted(binary.project.guid, binary.guid)
  }

  def findByProjectGuidAndNameAndVersion(
    auth: Authorization,
    projectGuid: UUID,
    name: String,
    version: String
  ): Option[ProjectBinary] = {
    findAll(
      auth,
      projectGuid = Some(projectGuid),
      name = Some(name),
      version = Some(version),
      limit = 1
    ).headOption
  }

  def findByGuid(auth: Authorization, guid: UUID): Option[ProjectBinary] = {
    findAll(auth, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    projectGuid: Option[UUID] = None,
    binaryGuid: Option[UUID] = None,
    name: Option[String] = None,
    version: Option[String] = None,
    isSynced: Option[Boolean] = None,
    hasBinary: Option[Boolean] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[ProjectBinary] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      Some(auth.organizations("organizations.guid", Some("projects.visibility")).and),
      guid.map { v => "and project_binaries.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("project_binaries.guid", _) },
      projectGuid.map { v => "and project_binaries.project_guid = {project_guid}::uuid" },
      binaryGuid.map { v => "and project_binaries.binary_guid = {binary_guid}::uuid" },
      name.map { v => "and lower(project_binaries.name) = lower(trim({name}))" },
      version.map { v => "and project_binaries.version = trim({version})" },
      isSynced.map { value =>
        val clause = "select 1 from syncs where object_guid = project_binaries.guid and event = {sync_event_completed}"
        value match {
          case true => s"and exists ($clause)"
          case false => s"and not exists ($clause)"
        }
      },
      hasBinary.map { value =>
        value match {
          case true => s"and project_binaries.binary_guid is not null"
          case false => s"and project_binaries.binary_guid is null"
        }
      },
      isDeleted.map(Filters.isDeleted("project_binaries", _)),
      Some(s"order by lower(project_binaries.name), project_binaries.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      binaryGuid.map('binary_guid -> _.toString),
      name.map('name -> _),
      version.map('version -> _.toString),
      isSynced.map(_ => ('sync_event_completed -> SyncEvent.Completed.toString))
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.ProjectBinary.table("project_binaries").*
      )
    }
  }

}

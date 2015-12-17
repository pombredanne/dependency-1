package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.api.lib.Version
import com.bryzek.dependency.v0.models.ProjectLibrary
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class ProjectLibraryForm(
  projectGuid: UUID,
  groupId: String,
  artifactId: String,
  version: String,
  crossBuildVersion: Option[String],
  path: String
)

object ProjectLibrariesDao {

  private[this] val BaseQuery = s"""
    select project_libraries.guid,
           project_libraries.group_id,
           project_libraries.artifact_id,
           project_libraries.version,
           project_libraries.cross_build_version,
           project_libraries.path,
           ${AuditsDao.all("project_libraries")},
           projects.guid as project_libraries_project_guid,
           projects.name as project_libraries_project_name,
           organizations.guid as project_libraries_project_organization_guid,
           organizations.key as project_libraries_project_organization_key
      from project_libraries
      join projects on projects.deleted_at is null and projects.guid = project_libraries.project_guid
      join organizations on organizations.deleted_at is null and organizations.guid = projects.organization_guid
     where true
  """

  private[this] val InsertQuery = """
    insert into project_libraries
    (guid, project_guid, group_id, artifact_id, version, cross_build_version, path, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {project_guid}::uuid, {group_id}, {artifact_id}, {version}, {cross_build_version}, {path}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[db] def validate(
    user: User,
    form: ProjectLibraryForm
  ): Seq[String] = {
    val groupIdErrors = if (form.groupId.trim.isEmpty) {
      Seq("Group ID cannot be empty")
    } else {
      Nil
    }

    val artifactIdErrors = if (form.artifactId.trim.isEmpty) {
      Seq("Artifact ID cannot be empty")
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

    val existsErrors = if (groupIdErrors.isEmpty && artifactIdErrors.isEmpty && versionErrors.isEmpty) {
      ProjectLibrariesDao.findByGroupIdAndArtifactIdAndVersionAndCrossBuildVersion(
        Authorization.All, form.groupId, form.artifactId, form.version, form.crossBuildVersion
      ) match {
        case None => Nil
        case Some(lib) => {
          Seq("Project library with this group id, artifact id, and version already exists")
        }
      }
    } else {
      Nil
    }

    projectErrors ++ groupIdErrors ++ artifactIdErrors ++ versionErrors ++ existsErrors
  }

  def upsert(createdBy: User, form: ProjectLibraryForm): Either[Seq[String], ProjectLibrary] = {
    ProjectLibrariesDao.findByGroupIdAndArtifactIdAndVersionAndCrossBuildVersion(
      Authorization.All, form.groupId, form.artifactId, form.version, form.crossBuildVersion
    ) match {
      case None => {
        create(createdBy, form)
      }
      case Some(lib) => {
        Right(lib)
      }
    }
  }

  def create(createdBy: User, form: ProjectLibraryForm): Either[Seq[String], ProjectLibrary] = {
    validate(createdBy, form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'project_guid -> form.projectGuid,
            'group_id -> form.groupId.trim,
            'artifact_id -> form.artifactId.trim,
            'version -> form.version.trim,
            'cross_build_version -> Util.trimmedString(form.crossBuildVersion),
            'path -> form.path.trim,
            'created_by_guid -> createdBy.guid
          ).execute()
          MainActor.ref ! MainActor.Messages.ProjectLibraryCreated(form.projectGuid, guid)
        }

        Right(
          findByGuid(Authorization.All, guid).getOrElse {
            sys.error("Failed to create project library")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, library: ProjectLibrary) {
    SoftDelete.delete("project_libraries", deletedBy.guid, library.guid)
    MainActor.ref ! MainActor.Messages.ProjectLibraryDeleted(library.project.guid, library.guid)
  }

  def findByGroupIdAndArtifactIdAndVersionAndCrossBuildVersion(
    auth: Authorization,
    groupId: String,
    artifactId: String,
    version: String,
    crossBuildVersion: Option[String]
  ): Option[ProjectLibrary] = {
    findAll(
      auth,
      groupId = Some(groupId),
      artifactId = Some(artifactId),
      version = Some(version),
      crossBuildVersion = Some(crossBuildVersion),
      limit = 1
    ).headOption
  }

  def findByGuid(auth: Authorization, guid: UUID): Option[ProjectLibrary] = {
    findAll(auth, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    projectGuid: Option[UUID] = None,
    groupId: Option[String] = None,
    artifactId: Option[String] = None,
    version: Option[String] = None,
    crossBuildVersion: Option[Option[String]] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[ProjectLibrary] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      Some(auth.organizations("organizations.guid", Some("projects.visibility")).and),
      guid.map { v => "and project_libraries.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("project_libraries.guid", _) },
      projectGuid.map { v => "and project_libraries.project_guid = {project_guid}::uuid" },
      groupId.map { v => "and lower(project_libraries.group_id) = lower(trim({group_id}))" },
      artifactId.map { v => "and lower(project_libraries.artifact_id) = lower(trim({artifact_id}))" },
      version.map { v => "and project_libraries.version = trim({version})" },
      crossBuildVersion.map { v =>
        v match {
          case None => "and project_libraries.cross_build_version is null"
          case Some(_) => "and project_libraries.cross_build_version = {cross_build_version}"
        }
      },
      isDeleted.map(Filters.isDeleted("project_libraries", _)),
      Some(s"order by lower(project_libraries.group_id), lower(project_libraries.artifact_id), project_libraries.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      groupId.map('group_id -> _),
      artifactId.map('artifact_id -> _),
      version.map('version -> _.toString),
      crossBuildVersion.flatMap { cbv =>
        cbv.map('cross_build_version -> _.toString)
      }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.ProjectLibrary.table("project_libraries").*
      )
    }
  }

}

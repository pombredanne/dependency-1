package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.{Scms, Binary, BinaryForm, Library, LibraryForm, Project, ProjectForm, ProjectSummary, OrganizationSummary, Visibility}
import com.bryzek.dependency.api.lib.GithubUtil
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object ProjectsDao {

  private[this] val BaseQuery = Query(s"""
    select projects.guid,
           projects.visibility,
           projects.scms,
           projects.name,
           projects.uri,
           ${AuditsDao.all("projects")},
           organizations.guid as projects_organization_guid,
           organizations.key as projects_organization_key
      from projects
      left join organizations on organizations.deleted_at is null and organizations.guid = projects.organization_guid
  """)

  private[this] val FilterProjectLibraries = """
    projects.guid in (select project_guid from project_libraries where deleted_at is null and %s)
  """.trim

  private[this] val FilterProjectBinaries = """
    projects.guid in (select project_guid from project_binaries where deleted_at is null and %s)
  """.trim

  private[this] val InsertQuery = """
    insert into projects
    (guid, organization_guid, visibility, scms, name, uri, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {visibility}, {scms}, {name}, {uri}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val UpdateQuery = """
    update projects
       set visibility = {visibility},
           scms = {scms},
           name = {name},
           uri = {uri},
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
  """

  def toSummary(project: Project): ProjectSummary = {
    ProjectSummary(
      guid = project.guid,
      organization = OrganizationSummary(project.organization.guid, project.organization.key),
      name = project.name
    )
  }

  private[db] def validate(
    user: User,
    form: ProjectForm,
    existing: Option[Project] = None
  ): Seq[String] = {
    val uriErrors = if (form.uri.trim == "") {
      Seq("Uri cannot be empty")
    } else {
      form.scms match {
        case Scms.UNDEFINED(_) => Seq("Scms not found")
        case Scms.Github => {
          GithubUtil.parseUri(form.uri) match {
            case Left(error) => Seq(error)
            case Right(_) => Nil
          }
        }
      }
    }

    val visibilityErrors = form.visibility match {
      case Visibility.UNDEFINED(_) => Seq("Visibility must be one of: ${Visibility.all.map(_.toString).mkString(", ")}")
      case _ => Nil
    }

    val nameErrors = if (form.name.trim == "") {
      Seq("Name cannot be empty")

    } else {
      ProjectsDao.findByOrganizationGuidAndName(Authorization.All, form.organizationGuid, form.name) match {
        case None => Seq.empty
        case Some(p) => {
          Some(p.guid) == existing.map(_.guid) match {
            case true => Nil
            case false => Seq("Project with this name already exists")
          }
        }
      }
    }

    val organizationErrors = MembershipsDao.isMember(form.organizationGuid, user) match  {
      case false => Seq("You do not have access to this organization")
      case true => Nil
    }

    nameErrors ++ visibilityErrors ++ uriErrors ++ organizationErrors
  }

  def create(createdBy: User, form: ProjectForm): Either[Seq[String], Project] = {
    validate(createdBy, form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'organization_guid -> form.organizationGuid,
            'visibility -> form.visibility.toString,
            'scms -> form.scms.toString,
            'name -> form.name.trim,
            'uri -> form.uri.trim,
            'created_by_guid -> createdBy.guid
          ).execute()
        }

        MainActor.ref ! MainActor.Messages.ProjectCreated(guid)

        Right(
          findByGuid(Authorization.All, guid).getOrElse {
            sys.error("Failed to create project")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def update(createdBy: User, project: Project, form: ProjectForm): Either[Seq[String], Project] = {
    validate(createdBy, form, Some(project)) match {
      case Nil => {
        // To support org change - need to record the change as its
        // own record to be able to track changes.
        assert(
          project.organization.guid == form.organizationGuid,
          "Changing organization not currently supported"
        )

        DB.withConnection { implicit c =>
          SQL(UpdateQuery).on(
            'guid -> project.guid,
            'visibility -> form.visibility.toString,
            'scms -> form.scms.toString,
            'name -> form.name.trim,
            'uri -> form.uri.trim,
            'updated_by_guid -> createdBy.guid
          ).execute()
        }

        MainActor.ref ! MainActor.Messages.ProjectUpdated(project.guid)

        Right(
          findByGuid(Authorization.All, project.guid).getOrElse {
            sys.error("Failed to create project")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, project: Project) {
    SoftDelete.delete("projects", deletedBy.guid, project.guid)
    MainActor.ref ! MainActor.Messages.ProjectDeleted(project.guid)
  }

  def findByOrganizationGuidAndName(auth: Authorization, organizationGuid: UUID, name: String): Option[Project] = {
    findAll(auth, organizationGuid = Some(organizationGuid), name = Some(name), limit = 1).headOption
  }

  def findByGuid(auth: Authorization, guid: UUID): Option[Project] = {
    findAll(auth, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    org: Option[String] = None,
    organizationGuid: Option[UUID] = None,
    name: Option[String] = None,
    groupId: Option[String] = None,
    artifactId: Option[String] = None,
    version: Option[String] = None,
    libraryGuid: Option[_root_.java.util.UUID] = None,
    binary: Option[String] = None,
    binaryGuid: Option[_root_.java.util.UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy.parseOrError("lower(projects.name), projects.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Project] = {

    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "projects",
        auth = auth.organizations("projects.organization_guid", Some("projects.visibility")),
        guid = guid,
        guids = guids,
        orderBy = orderBy,
        isDeleted = isDeleted,
        limit = limit,
        offset = offset
      ).
        text(
          "organizations.key",
          org,
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        uuid("organizations.guid", organizationGuid).
        text(
          "projects.name",
          name,
          columnFunctions = Seq(Query.Function.Lower),
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        condition(
          groupId.map { v => FilterProjectLibraries.format("project_libraries.group_id = trim({group_id})") }
        ).bind("group_id", groupId).
        condition(
          artifactId.map { v => FilterProjectLibraries.format("project_libraries.artifact_id = trim({artifact_id})") }
        ).bind("artifact_id", artifactId).
        condition(
          version.map { v => FilterProjectLibraries.format("project_libraries.version = trim({version})") }
        ).bind("version", version).
        condition(
          libraryGuid.map { v => FilterProjectLibraries.format("project_libraries.library_guid = {library_guid}::uuid") }
        ).bind("library_guid", libraryGuid).
        condition(
          binary.map { v => FilterProjectBinaries.format("project_binaries.name = trim({binary})") }
        ).bind("binary", binary).
        condition(
          binaryGuid.map { v => FilterProjectBinaries.format("project_binaries.binary_guid = {binary_guid}::uuid") }
        ).bind("binary_guid", binaryGuid).
        as(
          com.bryzek.dependency.v0.anorm.parsers.Project.table("projects").*
        )
    }
  }

}

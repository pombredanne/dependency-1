package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.{Scms, Binary, BinaryForm, Library, LibraryForm, Project, ProjectForm, Visibility}
import com.bryzek.dependency.api.lib.GithubUtil
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object ProjectsDao {

  private[this] val BaseQuery = s"""
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
     where true
  """

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

  private[this] val InsertLibraryVersionQuery = """
    insert into project_library_versions
    (guid, project_guid, library_version_guid, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {project_guid}::uuid, {library_version_guid}::uuid, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val InsertBinaryVersionQuery = """
    insert into project_binary_versions
    (guid, project_guid, binary_version_guid, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {project_guid}::uuid, {binary_version_guid}::uuid, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[db] def validate(
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
      ProjectsDao.findByOrganizationGuidAndName(form.organizationGuid, form.name) match {
        case None => Seq.empty
        case Some(p) => {
          Some(p.guid) == existing.map(_.guid) match {
            case true => Nil
            case false => Seq("Project with this name already exists")
          }
        }
      }
    }

    nameErrors ++ visibilityErrors ++ uriErrors
  }

  def setDependencies(
    createdBy: User,
    project: Project,
    binaries: Option[Seq[BinaryForm]] = None,
    libraries: Option[Seq[LibraryForm]] = None
  ) {
    DB.withTransaction { implicit c =>
      binaries.map { setBinaryVersions(c, createdBy, project, _) }
      libraries.map { setLibraryVersions(c, createdBy, project, _) }
    }
  }

  private[this] def setBinaryVersions(
    implicit c: java.sql.Connection,
    createdBy: User,
    project: Project,
    binaries: Seq[BinaryForm]
  ) {
    val newGuids = binaries.map { form =>
      val binary = BinariesDao.upsert(createdBy, form) match {
        case Left(errors) => sys.error(errors.mkString(", n"))
        case Right(binary) => binary
      }
      BinaryVersionsDao.findByBinaryAndVersion(binary, form.version).getOrElse {
        sys.error("Could not create binary version")
      }.guid
    }

    val existingGuids = BinaryVersionsDao.findAll(projectGuid = Some(project.guid)).map(_.guid)

    val toAdd = newGuids.filter { guid => !existingGuids.contains(guid) }
    val toRemove = existingGuids.filter { guid => !newGuids.contains(guid) }

    toAdd.distinct.foreach { guid =>
      SQL(InsertBinaryVersionQuery).on(
        'guid -> UUID.randomUUID,
        'project_guid -> project.guid,
        'binary_version_guid -> guid,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    toRemove.distinct.foreach { guid =>
      SoftDelete.delete(c, "project_binary_versions", createdBy.guid, ("binary_version_guid", Some("::uuid"), guid.toString))
    }
  }

  private[this] def setLibraryVersions(
    implicit c: java.sql.Connection,
    createdBy: User,
    project: Project,
    libraries: Seq[LibraryForm]
  ) {
    val newGuids = libraries.flatMap { form =>
      val library = LibrariesDao.upsert(createdBy, form) match {
        case Left(errors) => sys.error(errors.mkString(", n"))
        case Right(library) => library
      }
      form.version.map { version =>
        LibraryVersionsDao.findByLibraryAndVersionAndCrossBuildVersion(library, version.version, version.crossBuildVersion).getOrElse {
          sys.error("Could not create library version")
        }.guid
      }
    }

    val existingGuids = LibraryVersionsDao.findAll(projectGuid = Some(project.guid)).map(_.guid)

    val toAdd = newGuids.filter { guid => !existingGuids.contains(guid) }
    val toRemove = existingGuids.filter { guid => !newGuids.contains(guid) }

    toAdd.distinct.foreach { guid =>
      SQL(InsertLibraryVersionQuery).on(
        'guid -> UUID.randomUUID,
        'project_guid -> project.guid,
        'library_version_guid -> guid,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    toRemove.distinct.foreach { guid =>
      SoftDelete.delete(c, "project_library_versions", createdBy.guid, ("library_version_guid", Some("::uuid"), guid.toString))
    }
  }

  def create(createdBy: User, form: ProjectForm): Either[Seq[String], Project] = {
    validate(form) match {
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
          findByGuid(guid).getOrElse {
            sys.error("Failed to create project")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def update(createdBy: User, project: Project, form: ProjectForm): Either[Seq[String], Project] = {
    validate(form, Some(project)) match {
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
          findByGuid(project.guid).getOrElse {
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

  def findByOrganizationGuidAndName(organizationGuid: UUID, name: String): Option[Project] = {
    findAll(organizationGuid = Some(organizationGuid), name = Some(name), limit = 1).headOption
  }

  def findByGuid(guid: UUID): Option[Project] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  private val FilterLibraryVersions = """
    and projects.guid in (select project_library_versions.project_guid
                            from project_library_versions
                            join library_versions on library_versions.deleted_at is null
                                                 and library_versions.guid = project_library_versions.library_version_guid
                            join libraries on libraries.deleted_at is null
                                                 and libraries.guid = library_versions.library_guid
                           where project_library_versions.deleted_at is null
                             and %s)
  """.trim

  private val FilterBinaryVersions = """
    and projects.guid in (select project_binary_versions.project_guid
                            from project_binary_versions
                            join binary_versions on binary_versions.deleted_at is null
                                                 and binary_versions.guid = project_binary_versions.binary_version_guid
                            join binaries on binaries.deleted_at is null
                                                 and binaries.guid = binary_versions.binary_guid
                           where project_binary_versions.deleted_at is null
                             and %s)
  """.trim

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    organizationGuid: Option[UUID] = None,
    name: Option[String] = None,
    groupId: Option[String] = None,
    artifactId: Option[String] = None,
    version: Option[String] = None,
    libraryGuid: Option[_root_.java.util.UUID] = None,
    libraryVersionGuid: Option[_root_.java.util.UUID] = None,
    binary: Option[String] = None,
    binaryGuid: Option[_root_.java.util.UUID] = None,
    binaryVersionGuid: Option[_root_.java.util.UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Project] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and projects.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("projects.guid", _) },
      organizationGuid.map { v => "and projects.organization_guid = {organization_guid}::uuid" },
      name.map { v => "and lower(projects.name) = lower(trim({name}))" },
      groupId.map { v => FilterLibraryVersions.format("libraries.group_id = trim({group_id})") },
      artifactId.map { v => FilterLibraryVersions.format("libraries.artifact_id = trim({artifact_id})") },
      version.map { v => FilterLibraryVersions.format("library_versions.version = trim({version})") },
      libraryGuid.map { v => FilterLibraryVersions.format("libraries.guid = {library_guid}::uuid") },
      libraryVersionGuid.map { v => FilterLibraryVersions.format("library_versions.guid = {library_version_guid}::uuid") },
      binary.map { v => FilterBinaryVersions.format("lower(binaries.name) = lower(trim({binary}))") },
      binaryGuid.map { v => FilterBinaryVersions.format("binaries.guid = {binary_guid}::uuid") },
      binaryVersionGuid.map { v => FilterBinaryVersions.format("binary_versions.guid = {binary_version_guid}::uuid") },
      isDeleted.map(Filters.isDeleted("projects", _)),
      Some(s"order by projects.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      organizationGuid.map('organization_guid -> _.toString),
      name.map('name -> _.toString),
      groupId.map('group_id -> _.toString),
      artifactId.map('artifact_id -> _.toString),
      version.map('version -> _.toString),
      libraryGuid.map('library_guid -> _.toString),
      libraryVersionGuid.map('library_version_guid -> _.toString),
      binary.map('binary -> _.toString),
      binaryGuid.map('binary_guid -> _.toString),
      binaryVersionGuid.map('binary_version_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Project.table("projects").*
      )
    }
  }

}

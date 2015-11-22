package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.{Scms, Language, LanguageForm, Library, LibraryForm, Project, ProjectForm}
import com.bryzek.dependency.lib.GitHubUtil
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
           projects.scms,
           projects.name,
           projects.uri,
           ${AuditsDao.all("projects")}
      from projects
     where true
  """

  private[this] val InsertQuery = """
    insert into projects
    (guid, scms, name, uri, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {scms}, {name}, {uri}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val UpdateQuery = """
    update projects
       set scms = {scms},
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

  private[this] val InsertLanguageVersionQuery = """
    insert into project_language_versions
    (guid, project_guid, language_version_guid, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {project_guid}::uuid, {language_version_guid}::uuid, {created_by_guid}::uuid, {created_by_guid}::uuid)
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
        case Scms.GitHub => {
          GitHubUtil.parseUri(form.uri) match {
            case Left(error) => Seq(error)
            case Right(_) => Nil
          }
        }
      }
    }

    val nameErrors = if (form.name.trim == "") {
      Seq("Name cannot be empty")

    } else {
      ProjectsDao.findByName(form.name) match {
        case None => Seq.empty
        case Some(p) => {
          Some(p.guid) == existing.map(_.guid) match {
            case true => Nil
            case false => Seq("Project with this name already exists")
          }
        }
      }
    }

    nameErrors ++ uriErrors
  }

  def setDependencies(
    createdBy: User,
    project: Project,
    languages: Option[Seq[LanguageForm]] = None,
    libraries: Option[Seq[LibraryForm]] = None
  ) {
    DB.withTransaction { implicit c =>
      languages.map { setLanguageVersions(c, createdBy, project, _) }
      libraries.map { setLibraryVersions(c, createdBy, project, _) }
    }
  }

  private[this] def setLanguageVersions(
    implicit c: java.sql.Connection,
    createdBy: User,
    project: Project,
    languages: Seq[LanguageForm]
  ) {
    val newGuids = languages.map { form =>
      val lang = LanguagesDao.upsert(createdBy, form) match {
        case Left(errors) => sys.error(errors.mkString(", n"))
        case Right(lang) => lang
      }
      LanguageVersionsDao.findByLanguageAndVersion(lang, form.version).getOrElse {
        sys.error("Could not create language version")
      }.guid
    }

    val existingGuids = LanguagesDao.findAll(projectGuid = Some(project.guid)).map(_.guid)
    val toAdd = newGuids.filter { guid => !existingGuids.contains(guid) }
    val toRemove = existingGuids.filter { guid => !newGuids.contains(guid) }

    toAdd.foreach { guid =>
      SQL(InsertLanguageVersionQuery).on(
        'guid -> UUID.randomUUID,
        'project_guid -> project.guid,
        'language_version_guid -> guid,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    toRemove.foreach { guid =>
      SoftDelete.delete("project_language_versions", createdBy.guid, guid)
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

    toAdd.foreach { guid =>
      SQL(InsertLibraryVersionQuery).on(
        'guid -> UUID.randomUUID,
        'project_guid -> project.guid,
        'library_version_guid -> guid,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    toRemove.foreach { guid =>
      SoftDelete.delete("project_library_versions", createdBy.guid, guid)
    }
  }

  def create(createdBy: User, form: ProjectForm): Either[Seq[String], Project] = {
    validate(form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
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
        DB.withConnection { implicit c =>
          SQL(UpdateQuery).on(
            'guid -> project.guid,
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

  def findByName(name: String): Option[Project] = {
    findAll(name = Some(name), limit = 1).headOption
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

  private val FilterLanguageVersions = """
    and projects.guid in (select project_language_versions.project_guid
                            from project_language_versions
                            join language_versions on language_versions.deleted_at is null
                                                 and language_versions.guid = project_language_versions.language_version_guid
                            join languages on languages.deleted_at is null
                                                 and languages.guid = language_versions.language_guid
                           where project_language_versions.deleted_at is null
                             and %s)
  """.trim

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    name: Option[String] = None,
    groupId: _root_.scala.Option[String] = None,
    artifactId: _root_.scala.Option[String] = None,
    version: _root_.scala.Option[String] = None,
    libraryGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    libraryVersionGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    language: _root_.scala.Option[String] = None,
    languageGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    languageVersionGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Project] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and projects.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("projects.guid", _) },
      name.map { v => "and lower(projects.name) = lower(trim({name}))" },
      groupId.map { v => FilterLibraryVersions.format("libraries.group_id = trim({group_id})") },
      artifactId.map { v => FilterLibraryVersions.format("libraries.artifact_id = trim({artifact_id})") },
      version.map { v => FilterLibraryVersions.format("library_versions.version = trim({version})") },
      libraryGuid.map { v => FilterLibraryVersions.format("libraries.guid = {library_guid}::uuid") },
      libraryVersionGuid.map { v => FilterLibraryVersions.format("library_versions.guid = {library_version_guid}::uuid") },
      language.map { v => FilterLanguageVersions.format("lower(languages.name) = lower(trim({language}))") },
      languageGuid.map { v => FilterLanguageVersions.format("languages.guid = {language_guid}::uuid") },
      languageVersionGuid.map { v => FilterLanguageVersions.format("language_versions.guid = {language_version_guid}::uuid") },
      isDeleted.map(Filters.isDeleted("projects", _)),
      Some(s"order by projects.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      name.map('name -> _.toString),
      groupId.map('group_id -> _.toString),
      artifactId.map('artifact_id -> _.toString),
      version.map('version -> _.toString),
      libraryGuid.map('library_guid -> _.toString),
      libraryVersionGuid.map('library_version_guid -> _.toString),
      language.map('language -> _.toString),
      languageGuid.map('language_guid -> _.toString),
      languageVersionGuid.map('language_version_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Project.table("projects").*
      )
    }
  }

}

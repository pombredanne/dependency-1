package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.{Scms, Language, LanguageForm, Library, LibraryForm, Project, ProjectForm, User}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.play.util.ValidatedForm
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
           ${AuditsDao.query("projects")}
      from projects
     where true
  """

  private[this] val InsertQuery = """
    insert into projects
    (guid, scms, name, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {scms}, {name}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val InsertLibraryQuery = """
    insert into project_libraries
    (guid, project_guid, library_guid, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {project_guid}:uuid, library_guid}::uuid, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val SoftDeleteLibraryQuery = """
    update project_libraries
       set deleted_at = now(),
           deleted_by_guid = {deleted_by_guid}::uuid
     where project_guid = {project_guid}::uuid
       and library_guid = {library_guid}::uuid
  """

  private[this] val InsertLanguageQuery = """
    insert into project_libraries
    (guid, project_guid, language_guid, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {project_guid}:uuid, language_guid}::uuid, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val SoftDeleteLanguageQuery = """
    update project_libraries
       set deleted_at = now(),
           deleted_by_guid = {deleted_by_guid}::uuid
     where project_guid = {project_guid}::uuid
       and language_guid = {language_guid}::uuid
  """

  def validate(
    form: ProjectForm
  ): ValidatedForm[ProjectForm] = {
    val scmsErrors = form.scms match {
      case Scms.UNDEFINED(_) => Seq("Scms not found")
      case _ => Seq.empty
    }

    val nameErrors = if (form.name.trim == "") {
      Seq("Name address cannot be empty")

    } else {
      ProjectsDao.findAll(
        name = Some(form.name),
        limit = 1
      ).headOption match {
        case None => Seq.empty
        case Some(_) => Seq("Project with this name already exists")
      }
    }

    ValidatedForm(form, scmsErrors ++ nameErrors)
  }

  def setDependencies(
    createdBy: User,
    project: Project,
    languages: Option[Seq[LanguageForm]] = None,
    libraries: Option[Seq[LibraryForm]] = None
  ) {
    DB.withTransaction { implicit c =>
      languages.map { setLanguages(c, createdBy, project, _) }
      libraries.map { setLibraries(c, createdBy, project, _) }
    }
  }

  private[this] def setLanguages(
    implicit c: java.sql.Connection,
    createdBy: User,
    project: Project,
    languages: Seq[LanguageForm]
  ) {
    val newGuids = languages.map { language =>
      LanguagesDao.upsert(createdBy, language).guid
    }

    val existingGuids = LanguagesDao.findAll(projectGuid = Some(project.guid)).map(_.guid)

    val toAdd = newGuids.filter { guid => !existingGuids.contains(guid) }
    val toRemove = existingGuids.filter { guid => !newGuids.contains(guid) }

    toAdd.foreach { guid =>
      SQL(InsertLanguageQuery).on(
        'guid -> UUID.randomUUID,
        'project_guid -> project.guid,
        'language_guid -> guid,
        'created_by_guid -> createdBy.guid
      ).execute()

      SQL(SoftDeleteLanguageQuery).on(
        'project_guid -> project.guid,
        'language_guid -> guid,
        'deleted_by_guid -> createdBy.guid
      ).execute()
    }
  }

  private[this] def setLibraries(
    implicit c: java.sql.Connection,
    createdBy: User,
    project: Project,
    libraries: Seq[LibraryForm]
  ) {
    val newGuids = libraries.map { library =>
      LibrariesDao.upsert(createdBy, library).guid
    }

    val existingGuids = LibrariesDao.findAll(projectGuid = Some(project.guid)).map(_.guid)

    val toAdd = newGuids.filter { guid => !existingGuids.contains(guid) }
    val toRemove = existingGuids.filter { guid => !newGuids.contains(guid) }

    toAdd.foreach { guid =>
      SQL(InsertLibraryQuery).on(
        'guid -> UUID.randomUUID,
        'project_guid -> project.guid,
        'library_guid -> guid,
        'created_by_guid -> createdBy.guid
      ).execute()

      SQL(SoftDeleteLibraryQuery).on(
        'project_guid -> project.guid,
        'library_guid -> guid,
        'deleted_by_guid -> createdBy.guid
      ).execute()
    }
  }

  def create(createdBy: User, valid: ValidatedForm[ProjectForm]): Project = {
    valid.assertValid()

    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'scms -> valid.form.scms.toString,
        'name -> valid.form.name.trim,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    MainActor.ref ! MainActor.Messages.ProjectCreated(guid)

    findByGuid(guid).getOrElse {
      sys.error("Failed to create project")
    }
  }

  def softDelete(deletedBy: User, project: Project) {
    SoftDelete.delete("projects", deletedBy.guid, project.guid)
    MainActor.ref ! MainActor.Messages.ProjectDeleted(project.guid)
  }

  def findByGuid(guid: UUID): Option[Project] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    name: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Project] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      name.map { v => "and lower(projects.name) = lower(trim({name}))" },
      isDeleted.map(Filters.isDeleted("projects", _)),
      Some(s"order by projects.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      name.map('name -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Project = {
    Project(
      guid = row[UUID]("guid"),
      scms = Scms(row[String]("scms")),
      name = row[String]("name"),
      audit = AuditsDao.fromRowCreation(row)
    )
  }

}

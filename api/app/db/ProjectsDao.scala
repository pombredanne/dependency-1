package db

import com.bryzek.dependency.v0.models.{Scms, Project, ProjectForm, User}
import io.flow.common.v0.models.Error
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.play.util.{Validated, Validation}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object ProjectsDao {

  case class ValidatedProjectForm(form: ProjectForm, errors: Seq[Error]) extends Validated

  private[this] val BaseQuery = s"""
    select projects.guid,
           projects.name,
           projects.scms,
           projects.uri,
           ${AuditsDao.queryCreation("projects")}
      from projects
     where true
  """

  private[this] val InsertQuery = """
    insert into projects
    (guid, name, uri, scms, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {name}, {uri}, {scms}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def validate(
    form: ProjectForm
  ): Seq[Error] = {
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

    Validation.errors(scmsErrors ++ nameErrors)
  }

  def create(createdBy: User, valid: ValidatedProjectForm): Project = {
    valid.assertValid()

    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'name -> valid.form.name.trim,
        'scms -> valid.form.scms.toString,
        'uri -> valid.form.uri.trim,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create project")
    }
  }

  def softDelete(deletedBy: User, project: Project) {
    SoftDelete.delete("projects", deletedBy.guid, project.guid)
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
      name = row[String]("name"),
      scms = Scms(row[String]("scms")),
      uri = row[String]("uri"),
      audit = AuditsDao.fromRowCreation(row)
    )
  }

}

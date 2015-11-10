package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.{Library, LibraryForm, User}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.play.util.ValidatedForm
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object LibrariesDao {

  private[this] val BaseQuery = s"""
    select libraries.guid,
           libraries.group_id,
           libraries.artifact_id,
           ${AuditsDao.queryCreation("libraries")}
      from libraries
     where true
  """

  private[this] val InsertQuery = """
    insert into libraries
    (guid, group_id, artifact_id, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {group_id}, {artifact_id}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def validate(
    form: LibraryForm
  ): ValidatedForm[LibraryForm] = {
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

    val existsErrors = if (groupIdErrors.isEmpty && artifactIdErrors.isEmpty) {
      LibrariesDao.findByGroupIdAndArtifactId(form.groupId, form.artifactId) match {
        case None => Nil
        case Some(_) => Seq("group id and artifact id already exist")
      }
    } else {
      Nil
    }

    ValidatedForm(form, groupIdErrors ++ artifactIdErrors ++ existsErrors)
  }

  def upsert(createdBy: User, form: LibraryForm): Library = {
    LibrariesDao.findByGroupIdAndArtifactId(form.groupId, form.artifactId) match {
      case None => {
        create(createdBy, validate(form))
      }
      case Some(lib) => {
        Util.trimmedString(form.version).map { version =>
          // TODO: Create version
        }
        lib
      }
    }
  }

  def create(createdBy: User, valid: ValidatedForm[LibraryForm]): Library = {
    valid.assertValid()

    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'group_id -> valid.form.groupId.trim,
        'artifact_id -> valid.form.artifactId.trim,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    MainActor.ref ! MainActor.Messages.LibraryCreated(guid)

    findByGuid(guid).getOrElse {
      sys.error("Failed to create library")
    }
  }

  def softDelete(deletedBy: User, library: Library) {
    SoftDelete.delete("libraries", deletedBy.guid, library.guid)
    MainActor.ref ! MainActor.Messages.LibraryDeleted(library.guid)
  }

  def findByGroupIdAndArtifactId(groupId: String, artifactId: String): Option[Library] = {
    findAll(groupId = Some(groupId), artifactId = Some(artifactId), limit = 1).headOption
  }

  def findByGuid(guid: UUID): Option[Library] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    groupId: Option[String] = None,
    artifactId: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Library] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      projectGuid.map { v => "and libraries.guid in (select library_guid from project_libraries where project_guid = {project_guid}::uuid and deleted_at is null" },
      groupId.map { v => "and lower(libraries.group_id) = lower(trim({group_id}))" },
      artifactId.map { v => "and lower(libraries.artifact_id) = lower(trim({artifact_id}))" },
      isDeleted.map(Filters.isDeleted("libraries", _)),
      Some(s"order by libraries.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      groupId.map('group_id -> _.toString),
      artifactId.map('artifact_id -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Library = {
    Library(
      guid = row[UUID]("guid"),
      groupId = row[String]("group_id"),
      artifactId = row[String]("artifact_id"),
      audit = AuditsDao.fromRowCreation(row)
    )
  }

}

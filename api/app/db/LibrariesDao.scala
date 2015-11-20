package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.{Library, LibraryForm}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object LibrariesDao {

  private[this] val BaseQuery = s"""
    select libraries.guid,
           array_to_json(string_to_array(libraries.resolvers, ' ')) as resolvers,
           libraries.group_id,
           libraries.artifact_id,
           ${AuditsDao.all("libraries")}
      from libraries
     where true
  """

  private[this] val InsertQuery = """
    insert into libraries
    (guid, resolvers, group_id, artifact_id, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {resolvers}, {group_id}, {artifact_id}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[db] def validate(
    form: LibraryForm
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

    val existsErrors = if (groupIdErrors.isEmpty && artifactIdErrors.isEmpty) {
      LibrariesDao.findByResolversAndGroupIdAndArtifactId(form.resolvers, form.groupId, form.artifactId) match {
        case None => Nil
        case Some(_) => Seq("Library with these resolvers, group id and artifact id already exists")
      }
    } else {
      Nil
    }

    groupIdErrors ++ artifactIdErrors ++ existsErrors
  }

  def upsert(createdBy: User, form: LibraryForm): Either[Seq[String], Library] = {
    LibrariesDao.findByResolversAndGroupIdAndArtifactId(form.resolvers, form.groupId, form.artifactId) match {
      case None => {
        create(createdBy, form)
      }
      case Some(lib) => {
        Util.trimmedString(form.version).map { version =>
          LibraryVersionsDao.upsert(createdBy, lib.guid, version)
        }
        Right(lib)
      }
    }
  }

  def create(createdBy: User, form: LibraryForm): Either[Seq[String], Library] = {
    validate(form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'resolvers -> resolversToString(form.resolvers),
            'group_id -> form.groupId.trim,
            'artifact_id -> form.artifactId.trim,
            'created_by_guid -> createdBy.guid
          ).execute()
        }

        MainActor.ref ! MainActor.Messages.LibraryCreated(guid)

        Right(
          findByGuid(guid).getOrElse {
            sys.error("Failed to create library")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, library: Library) {
    SoftDelete.delete("libraries", deletedBy.guid, library.guid)
    MainActor.ref ! MainActor.Messages.LibraryDeleted(library.guid)
  }

  def findByResolversAndGroupIdAndArtifactId(
    resolvers: Seq[String],
    groupId: String,
    artifactId: String
  ): Option[Library] = {
    findAll(
      resolvers = Some(resolvers),
      groupId = Some(groupId),
      artifactId = Some(artifactId),
      limit = 1
    ).headOption
  }

  def findByGuid(guid: UUID): Option[Library] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    projectGuid: Option[UUID] = None,
    resolvers: Option[Seq[String]] = None,
    groupId: Option[String] = None,
    artifactId: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Library] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and libraries.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("libraries.guid", _) },
      projectGuid.map { v => "and libraries.guid in (select library_guid from project_libraries where project_guid = {project_guid}::uuid and deleted_at is null" },
      resolvers.map { v => "and libraries.resolvers = {resolvers}" },
      groupId.map { v => "and lower(libraries.group_id) = lower(trim({group_id}))" },
      artifactId.map { v => "and lower(libraries.artifact_id) = lower(trim({artifact_id}))" },
      isDeleted.map(Filters.isDeleted("libraries", _)),
      Some(s"order by libraries.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      resolvers.map(v => 'resolvers -> resolversToString(v)),
      groupId.map('group_id -> _.toString),
      artifactId.map('artifact_id -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Library.parser(
          com.bryzek.dependency.v0.anorm.parsers.Library.Mappings.table("libraries").copy(
            resolvers = "resolvers"
          )
        ).*
      )
    }
  }

  private[db] def resolversToString(resolvers: Seq[String]): String = {
    resolvers.map(_.trim).filter(!_.isEmpty).mkString(" ")
  }

}

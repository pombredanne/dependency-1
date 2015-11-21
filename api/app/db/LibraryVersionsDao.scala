package db

import com.bryzek.dependency.v0.models.{LanguageVersion, LibraryVersion}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object LibraryVersionsDao {

  private[this] val BaseQuery = s"""
    select library_versions.guid,
           library_versions.version,
           ${AuditsDao.all("library_versions")},
           libraries.guid as library_versions_library_guid,
           array_to_json(string_to_array(libraries.resolvers, ' ')) as library_versions_library_resolvers,
           libraries.group_id as library_versions_library_group_id,
           libraries.artifact_id as library_versions_library_artifact_id,
           ${AuditsDao.all("libraries", Some("library_versions_library"))}
      from library_versions
      join libraries on libraries.deleted_at is null and libraries.guid = library_versions.library_guid
     where true
  """

  private[this] val InsertQuery = s"""
    insert into library_versions
    (guid, library_guid, version, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {object_guid}::uuid, {version}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, objectGuid: UUID, version: String): LibraryVersion = {
    DB.withConnection { implicit c =>
      upsertWithConnection(createdBy, objectGuid, version)
    }
  }

  private[db] def upsertWithConnection(createdBy: User, objectGuid: UUID, version: String)(
    implicit c: java.sql.Connection
  ): LibraryVersion = {
    findAllWithConnection(
      objectGuid = Some(objectGuid),
      version = Some(version),
      limit = 1
    ).headOption.getOrElse {
      createWithConnection(createdBy, objectGuid, version)
    }
  }

  def create(createdBy: User, objectGuid: UUID, version: String): LibraryVersion = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, objectGuid, version)
    }
  }

  def createWithConnection(createdBy: User, objectGuid: UUID, version: String)(implicit c: java.sql.Connection): LibraryVersion = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'object_guid -> objectGuid,
      'version -> version,
      'created_by_guid -> createdBy.guid
    ).execute()

    findByGuidWithConnection(guid).getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(deletedBy: User, guid: UUID) {
    SoftDelete.delete("library_versions", deletedBy.guid, guid)
  }

  def findByObjectGuidAndVersion(
    objectGuid: UUID, version: String
  ) (
    implicit c: java.sql.Connection
  ): Option[LibraryVersion] = {
    findAllWithConnection(
      objectGuid = Some(objectGuid),
      version = Some(version),
      limit = 1
    ).headOption
  }

  def findByGuid(
    guid: UUID
  ): Option[LibraryVersion] = {
    DB.withConnection { implicit c =>
      findByGuidWithConnection(guid)
    }
  }

  def findByGuidWithConnection(
    guid: UUID
  ) (
    implicit c: java.sql.Connection
  ): Option[LibraryVersion] = {
    findAllWithConnection(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    objectGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    version: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ) = {
    DB.withConnection { implicit c =>
      findAllWithConnection(
        guid = guid,
        guids = guids,
        objectGuid = objectGuid,
        projectGuid = projectGuid,
        version = version,
        isDeleted = isDeleted,
        limit = limit,
        offset = offset
      )
    }
  }

  def findAllWithConnection(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    objectGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    version: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ) (
    implicit c: java.sql.Connection
  ): Seq[LibraryVersion] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => s"and library_versions.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids(s"library_versions.guid", _) },
      objectGuid.map { v => s"and library_versions.library_guid = {object_guid}::uuid" },
      projectGuid.map { v => s"and library_versions.guid in (select library_version_guid from project_library_versions where deleted_at is null and project_guid = {project_guid}::uuid)" },
      version.map { v => s"and lower(library_versions.version) = lower(trim({version}))" },
      isDeleted.map(Filters.isDeleted("library_versions", _)),
      Some(s"order by library_versions.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      objectGuid.map('object_guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      version.map('version -> _.toString)
    ).flatten

    SQL(sql).on(bind: _*).as(
      com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.table("library_versions").*
    )
  }

}


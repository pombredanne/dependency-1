package db

import com.bryzek.dependency.v0.models.{LanguageVersion, LibraryVersion}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

trait VersionsDao[T] {

  def tableName: String
  def columnName: String
  def mappingTableName: String
  def mappingColumnName: String

  private[db] def parser: RowParser[T]

  private[this] val BaseQuery = s"""
    select $tableName.guid,
           $tableName.version,
           ${AuditsDao.all(tableName)}
      from $tableName
     where true
  """

  private[this] val InsertQuery = s"""
    insert into $tableName
    (guid, $columnName, version, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {object_guid}::uuid, {version}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, objectGuid: UUID, version: String): T = {
    DB.withConnection { implicit c =>
      upsertWithConnection(createdBy, objectGuid, version)
    }
  }

  private[db] def upsertWithConnection(createdBy: User, objectGuid: UUID, version: String)(
    implicit c: java.sql.Connection
  ): T = {
    findAllWithConnection(
      objectGuid = Some(objectGuid),
      version = Some(version),
      limit = 1
    ).headOption.getOrElse {
      createWithConnection(createdBy, objectGuid, version)
    }
  }

  def create(createdBy: User, objectGuid: UUID, version: String): T = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, objectGuid, version)
    }
  }

  def createWithConnection(createdBy: User, objectGuid: UUID, version: String)(implicit c: java.sql.Connection): T = {
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
    SoftDelete.delete(tableName, deletedBy.guid, guid)
  }

  def findByObjectGuidAndVersion(
    objectGuid: UUID, version: String
  ) (
    implicit c: java.sql.Connection
  ): Option[T] = {
    findAllWithConnection(
      objectGuid = Some(objectGuid),
      version = Some(version),
      limit = 1
    ).headOption
  }

  def findByGuid(
    guid: UUID
  ): Option[T] = {
    DB.withConnection { implicit c =>
      findByGuidWithConnection(guid)
    }
  }

  def findByGuidWithConnection(
    guid: UUID
  ) (
    implicit c: java.sql.Connection
  ): Option[T] = {
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
  ): Seq[T] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => s"and $tableName.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids(s"$tableName.guid", _) },
      objectGuid.map { v => s"and $tableName.$columnName = {object_guid}::uuid" },
      projectGuid.map { v => s"and $tableName.$columnName in (select $mappingColumnName from $mappingTableName where deleted_at is null and project_guid = {project_guid}::uuid)" },
      version.map { v => s"and lower($tableName.version) = lower(trim({version}))" },
      isDeleted.map(Filters.isDeleted(tableName, _)),
      Some(s"order by $tableName.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      objectGuid.map('object_guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      version.map('version -> _.toString)
    ).flatten

    SQL(sql).on(bind: _*).as(parser.*)
  }

}

object LanguageVersionsDao extends VersionsDao[LanguageVersion] {

  override def tableName = "language_versions"
  override def columnName = "language_guid"
  override def mappingTableName = "project_language_versions"
  override def mappingColumnName = "language_version_guid"

  private[db] override def parser = com.bryzek.dependency.v0.anorm.parsers.LanguageVersion.table(tableName)

}

object LibraryVersionsDao extends VersionsDao[LibraryVersion] {

  override def tableName = "library_versions"
  override def columnName = "library_guid"
  override def mappingTableName = "project_library_versions"
  override def mappingColumnName = "library_version_guid"
  private[db] override def parser = com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.table(tableName)

}

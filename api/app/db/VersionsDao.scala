package db

import com.bryzek.dependency.v0.models.{LanguageVersion, LibraryVersion, User}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.play.util.ValidatedForm
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

trait VersionsDao[T] {

  def tableName: String

  def columnName: String

  private[this] val BaseQuery = s"""
    select $tableName.guid,
           $tableName.version,
           ${AuditsDao.query(tableName)}
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
    findAll(
      objectGuid = Some(objectGuid),
      version = Some(version),
      limit = 1
    ).headOption.getOrElse {
      create(createdBy, objectGuid, version)
    }
  }

  def create(createdBy: User, objectGuid: UUID, version: String): T = {
    DB.withConnection { implicit c =>
      create(c, createdBy, objectGuid, version)
    }
  }

  def create(implicit c: java.sql.Connection, createdBy: User, objectGuid: UUID, version: String): T = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'object_guid -> objectGuid,
      'version -> version,
      'created_by_guid -> createdBy.guid
    ).execute()

    findByGuid(guid).getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(deletedBy: User, guid: UUID) {
    SoftDelete.delete(tableName, deletedBy.guid, guid)
  }

  def findByGuid(guid: UUID): Option[T] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    objectGuid: Option[UUID] = None,
    version: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[T] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      objectGuid.map { v => s"and $tableName.$columnName = {object_guid}" },
      version.map { v => "and lower($tableName.version) = lower(trim({version}))" },
      isDeleted.map(Filters.isDeleted(tableName, _)),
      Some(s"order by $tableName.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      objectGuid.map('object_guid -> _.toString),
      version.map('version -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): T

}

object LanguageVersionsDao extends VersionsDao[LanguageVersion] {

  override def tableName = "language_versions"
  override def columnName = "version_guid"

  private[db] override def fromRow(
    row: anorm.Row
  ): LanguageVersion = {
    LanguageVersion(
      guid = row[UUID]("guid"),
      version = row[String]("version"),
      audit = AuditsDao.fromRowCreation(row)
    )
  }

}

object LibraryVersionsDao extends VersionsDao[LibraryVersion] {

  override def tableName = "library_versions"
  override def columnName = "version_guid"

  private[db] override def fromRow(
    row: anorm.Row
  ): LibraryVersion = {
    LibraryVersion(
      guid = row[UUID]("guid"),
      version = row[String]("version"),
      audit = AuditsDao.fromRowCreation(row)
    )
  }

}

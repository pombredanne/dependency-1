package db

import com.bryzek.dependency.api.lib.Version
import com.bryzek.dependency.v0.models.{Binary, BinaryType, BinaryVersion}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import scala.util.{Failure, Success, Try}

object BinaryVersionsDao {

  private[this] val BaseQuery = s"""
    select binary_versions.guid,
           binary_versions.version,
           ${AuditsDao.all("binary_versions")},
           binaries.guid as binary_versions_binary_guid,
           binaries.name as binary_versions_binary_name,
           ${AuditsDao.all("binaries", Some("binary_versions_binary"))},
           organizations.guid as binary_versions_binary_organization_guid,
           organizations.key as binary_versions_binary_organization_key
      from binary_versions
      join binaries on binaries.deleted_at is null and binaries.guid = binary_versions.binary_guid
      left join organizations on organizations.deleted_at is null and organizations.guid = binaries.organization_guid
     where true
  """

  private[this] val InsertQuery = s"""
    insert into binary_versions
    (guid, binary_guid, version, sort_key, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {binary_guid}::uuid, {version}, {sort_key}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, binaryGuid: UUID, version: String): BinaryVersion = {
    DB.withConnection { implicit c =>
      upsertWithConnection(createdBy, binaryGuid, version)
    }
  }

  private[db] def upsertWithConnection(createdBy: User, binaryGuid: UUID, version: String)(
    implicit c: java.sql.Connection
  ): BinaryVersion = {
    findAllWithConnection(
      binaryGuid = Some(binaryGuid),
      version = Some(version),
      limit = 1
    ).headOption.getOrElse {
      Try {
        createWithConnection(createdBy, binaryGuid, version)
      } match {
        case Success(version) => version
        case Failure(ex) => {
          findAllWithConnection(
            binaryGuid = Some(binaryGuid),
            version = Some(version),
            limit = 1
          ).headOption.getOrElse {
            play.api.Logger.error(ex.getMessage, ex)
            sys.error(ex.getMessage)
          }
        }
      }
    }
  }

  def create(createdBy: User, binaryGuid: UUID, version: String): BinaryVersion = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, binaryGuid, version)
    }
  }

  def createWithConnection(createdBy: User, binaryGuid: UUID, version: String)(implicit c: java.sql.Connection): BinaryVersion = {
    assert(!version.trim.isEmpty, "Version must be non empty")
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'binary_guid -> binaryGuid,
      'version -> version.trim,
      'sort_key -> Version(version.trim).sortKey,
      'created_by_guid -> createdBy.guid
    ).execute()

    findByGuidWithConnection(guid).getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(deletedBy: User, guid: UUID) {
    SoftDelete.delete("binary_versions", deletedBy.guid, guid)
  }

  def findByBinaryAndVersion(
    binary: Binary, version: String
  ): Option[BinaryVersion] = {
    findAll(
      binaryGuid = Some(binary.guid),
      version = Some(version),
      limit = 1
    ).headOption
  }

  def findByGuid(
    guid: UUID
  ): Option[BinaryVersion] = {
    DB.withConnection { implicit c =>
      findByGuidWithConnection(guid)
    }
  }

  def findByGuidWithConnection(
    guid: UUID
  ) (
    implicit c: java.sql.Connection
  ): Option[BinaryVersion] = {
    findAllWithConnection(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    binaryGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    version: Option[String] = None,
    greaterThanVersion: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ) = {
    DB.withConnection { implicit c =>
      findAllWithConnection(
        guid = guid,
        guids = guids,
        binaryGuid = binaryGuid,
        projectGuid = projectGuid,
        version = version,
        greaterThanVersion = greaterThanVersion,
        isDeleted = isDeleted,
        limit = limit,
        offset = offset
      )
    }
  }

  def findAllWithConnection(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    binaryGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    version: Option[String] = None,
    greaterThanVersion: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ) (
    implicit c: java.sql.Connection
  ): Seq[BinaryVersion] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => s"and binary_versions.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids(s"binary_versions.guid", _) },
      binaryGuid.map { v => s"and binary_versions.binary_guid = {binary_guid}::uuid" },
      projectGuid.map { v => s"and binary_versions.guid in (select binary_version_guid from project_binary_versions where deleted_at is null and project_guid = {project_guid}::uuid)" },
      version.map { v => s"and lower(binary_versions.version) = lower(trim({version}))" },
      greaterThanVersion.map { v =>
        s"and binary_versions.sort_key > {greater_than_version_sort_key}"
      },
      isDeleted.map(Filters.isDeleted("binary_versions", _)),
      Some(s"order by binary_versions.sort_key desc, binary_versions.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      binaryGuid.map('binary_guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      version.map('version -> _.toString),
      greaterThanVersion.map( v =>
        'greater_than_version_sort_key -> Version(v).sortKey
      )
    ).flatten

    SQL(sql).on(bind: _*).as(
      com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.table("binary_versions").*
    )
  }

}


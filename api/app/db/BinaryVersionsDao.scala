package db

import com.bryzek.dependency.api.lib.Version
import com.bryzek.dependency.v0.models.{Binary, BinaryType, BinaryVersion}
import io.flow.postgresql.{Query, OrderBy}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import scala.util.{Failure, Success, Try}

object BinaryVersionsDao {

  private[this] val BaseQuery = Query(s"""
    select binary_versions.guid,
           binary_versions.version,
           binaries.guid as binary_versions_binary_guid,
           binaries.name as binary_versions_binary_name,
           organizations.guid as binary_versions_binary_organization_guid,
           organizations.key as binary_versions_binary_organization_key
      from binary_versions
      join binaries on binaries.deleted_at is null and binaries.guid = binary_versions.binary_guid
      left join organizations on organizations.deleted_at is null and organizations.guid = binaries.organization_guid
  """)

  private[this] val InsertQuery = s"""
    insert into binary_versions
    (guid, binary_guid, version, sort_key, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {binary_guid}::uuid, {version}, {sort_key}, {updated_by_user_id})
  """

  def upsert(createdBy: User, binaryGuid: UUID, version: String): BinaryVersion = {
    DB.withConnection { implicit c =>
      upsertWithConnection(createdBy, binaryGuid, version)
    }
  }

  private[db] def upsertWithConnection(createdBy: User, binaryGuid: UUID, version: String)(
    implicit c: java.sql.Connection
  ): BinaryVersion = {
    val auth = Authorization.User(createdBy.guid)
    findAllWithConnection(
      auth,
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
            auth,
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
      'updated_by_user_id -> createdBy.id
    ).execute()

    findByGuidWithConnection(Authorization.All, guid).getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(deletedBy: User, guid: UUID) {
    SoftDelete.delete("binary_versions", deletedBy.guid, guid)
  }

  def findByBinaryAndVersion(
    auth: Authorization,
    binary: Binary, version: String
  ): Option[BinaryVersion] = {
    findAll(
      auth,
      binaryGuid = Some(binary.guid),
      version = Some(version),
      limit = 1
    ).headOption
  }

  def findByGuid(
    auth: Authorization,
    guid: UUID
  ): Option[BinaryVersion] = {
    DB.withConnection { implicit c =>
      findByGuidWithConnection(auth, guid)
    }
  }

  def findByGuidWithConnection(
    auth: Authorization,
    guid: UUID
  ) (
    implicit c: java.sql.Connection
  ): Option[BinaryVersion] = {
    findAllWithConnection(auth, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
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
        auth,
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
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    binaryGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    version: Option[String] = None,
    greaterThanVersion: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy(s"-binary_versions.sort_key, binary_versions.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ) (
    implicit c: java.sql.Connection
  ): Seq[BinaryVersion] = {
    // N.B.: at this time, all binary versions are public and thus we
    // do not need to filter by auth. It is here in the API for
    // consistency and to explicitly declare we are respecting it.

    BaseQuery.
      equals("binary_versions.guid", guid).
      in("binary_versions.guid", guids).
      equals("binary_versions.binary_guid", binaryGuid).
      subquery("binary_versions.binary_guid", "project_guid", projectGuid, { bind =>
        s"select binary_guid from project_binaries where deleted_at is null and binary_guid is not null and project_guid = ${bind.sql}"
      }).
      text(
        "binary_versions.version",
        version,
        columnFunctions = Seq(Query.Function.Lower),
        valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
      ).
      condition(
        greaterThanVersion.map { v =>
          s"binary_versions.sort_key > {greater_than_version_sort_key}"
        }
      ).
      bind("greater_than_version_sort_key", greaterThanVersion).
      nullBoolean("binary_versions.deleted_at", isDeleted).
      orderBy(orderBy.sql).
      limit(Some(limit)).
      offset(Some(offset)).
      as(
        com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.table("binary_versions").*
      )
  }

}


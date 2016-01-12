package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.api.lib.Version
import com.bryzek.dependency.v0.models.{Binary, BinaryType, BinaryVersion}
import io.flow.postgresql.{Query, OrderBy}
import io.flow.common.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import scala.util.{Failure, Success, Try}

object BinaryVersionsDao {

  private[this] val BaseQuery = Query(s"""
    select binary_versions.id,
           binary_versions.version,
           binaries.id as binary_versions_binary_id,
           binaries.name as binary_versions_binary_name,
           organizations.id as binary_versions_binary_organization_id,
           organizations.key as binary_versions_binary_organization_key
      from binary_versions
      join binaries on binaries.deleted_at is null and binaries.id = binary_versions.binary_id
      left join organizations on organizations.deleted_at is null and organizations.id = binaries.organization_id
  """)

  private[this] val InsertQuery = s"""
    insert into binary_versions
    (id, binary_id, version, sort_key, updated_by_user_id)
    values
    ({id}, {binary_id}, {version}, {sort_key}, {updated_by_user_id})
  """

  def upsert(createdBy: User, binaryId: String, version: String): BinaryVersion = {
    DB.withConnection { implicit c =>
      upsertWithConnection(createdBy, binaryId, version)
    }
  }

  private[db] def upsertWithConnection(createdBy: User, binaryId: String, version: String)(
    implicit c: java.sql.Connection
  ): BinaryVersion = {
    val auth = Authorization.User(createdBy.id)
    findAllWithConnection(
      auth,
      binaryId = Some(binaryId),
      version = Some(version),
      limit = 1
    ).headOption.getOrElse {
      Try {
        createWithConnection(createdBy, binaryId, version)
      } match {
        case Success(version) => version
        case Failure(ex) => {
          findAllWithConnection(
            auth,
            binaryId = Some(binaryId),
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

  def create(createdBy: User, binaryId: String, version: String): BinaryVersion = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, binaryId, version)
    }
  }

  def createWithConnection(createdBy: User, binaryId: String, version: String)(implicit c: java.sql.Connection): BinaryVersion = {
    assert(!version.trim.isEmpty, "Version must be non empty")
    val id = io.flow.play.util.IdGenerator("biv").randomId()

    SQL(InsertQuery).on(
      'id -> id,
      'binary_id -> binaryId,
      'version -> version.trim,
      'sort_key -> Version(version.trim).sortKey,
      'updated_by_user_id -> createdBy.id
    ).execute()

    MainActor.ref ! MainActor.Messages.BinaryVersionCreated(id)

    findByIdWithConnection(Authorization.All, id).getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(deletedBy: User, id: String) {
    SoftDelete.delete("binary_versions", deletedBy.id, id)
    MainActor.ref ! MainActor.Messages.BinaryVersionDeleted(id)
  }

  def findByBinaryAndVersion(
    auth: Authorization,
    binary: Binary, version: String
  ): Option[BinaryVersion] = {
    findAll(
      auth,
      binaryId = Some(binary.id),
      version = Some(version),
      limit = 1
    ).headOption
  }

  def findById(
    auth: Authorization,
    id: String
  ): Option[BinaryVersion] = {
    DB.withConnection { implicit c =>
      findByIdWithConnection(auth, id)
    }
  }

  def findByIdWithConnection(
    auth: Authorization,
    id: String
  ) (
    implicit c: java.sql.Connection
  ): Option[BinaryVersion] = {
    findAllWithConnection(auth, id = Some(id), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    binaryId: Option[String] = None,
    projectId: Option[String] = None,
    version: Option[String] = None,
    greaterThanVersion: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ) = {
    DB.withConnection { implicit c =>
      findAllWithConnection(
        auth,
        id = id,
        ids = ids,
        binaryId = binaryId,
        projectId = projectId,
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
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    binaryId: Option[String] = None,
    projectId: Option[String] = None,
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
      equals("binary_versions.id", id).
      in("binary_versions.id", ids).
      equals("binary_versions.binary_id", binaryId).
      subquery("binary_versions.binary_id", "project_id", projectId, { bind =>
        s"select binary_id from project_binaries where deleted_at is null and binary_id is not null and project_id = ${bind.sql}"
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


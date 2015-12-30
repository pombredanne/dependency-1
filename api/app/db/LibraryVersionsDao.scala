package db

import com.bryzek.dependency.api.lib.Version
import com.bryzek.dependency.v0.models.{Library, LibraryVersion, VersionForm}
import io.flow.postgresql.{Query, OrderBy}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import scala.util.{Failure, Success, Try}

object LibraryVersionsDao {

  private[this] val BaseQuery = Query(s"""
    select library_versions.guid,
           library_versions.version,
           library_versions.cross_build_version,
           libraries.guid as library_versions_library_guid,
           libraries.group_id as library_versions_library_group_id,
           libraries.artifact_id as library_versions_library_artifact_id,
           organizations.guid as library_versions_library_organization_guid,
           organizations.key as library_versions_library_organization_key,
           resolvers.guid as library_versions_library_resolver_guid,
           resolvers.visibility as library_versions_library_resolver_visibility,
           resolvers.uri as library_versions_library_resolver_uri,
           resolver_orgs.guid as library_versions_library_resolver_organization_guid,
           resolver_orgs.key as library_versions_library_resolver_organization_key
      from library_versions
      join libraries on libraries.deleted_at is null and libraries.guid = library_versions.library_guid
      join organizations on organizations.deleted_at is null and organizations.guid = libraries.organization_guid
      join resolvers on resolvers.deleted_at is null and resolvers.guid = libraries.resolver_guid
      left join organizations resolver_orgs on resolver_orgs.deleted_at is null and resolver_orgs.guid = resolvers.organization_guid
  """)

  private[this] val InsertQuery = s"""
    insert into library_versions
    (guid, library_guid, version, cross_build_version, sort_key, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {library_guid}::uuid, {version}, {cross_build_version}, {sort_key}, {updated_by_user_id})
  """

  def upsert(createdBy: User, libraryGuid: UUID, form: VersionForm): LibraryVersion = {
    DB.withConnection { implicit c =>
      upsertWithConnection(createdBy, libraryGuid, form)
    }
  }

  private[db] def upsertWithConnection(createdBy: User, libraryGuid: UUID, form: VersionForm)(
    implicit c: java.sql.Connection
  ): LibraryVersion = {
    val auth = Authorization.User(createdBy.id)

    findAllWithConnection(
      auth,
      libraryGuid = Some(libraryGuid),
      version = Some(form.version),
      crossBuildVersion = Some(form.crossBuildVersion),
      limit = 1
    ).headOption.getOrElse {
      Try {
        createWithConnection(createdBy, libraryGuid, form)
      } match {
        case Success(version) => version
        case Failure(ex) => {
          // check concurrent insert
          findAllWithConnection(
            auth,
            libraryGuid = Some(libraryGuid),
            version = Some(form.version),
            crossBuildVersion = Some(form.crossBuildVersion),
            limit = 1
          ).headOption.getOrElse {
            play.api.Logger.error(ex.getMessage, ex)
            sys.error(ex.getMessage)
          }
        }
      }
    }
  }

  def create(createdBy: User, libraryGuid: UUID, form: VersionForm): LibraryVersion = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, libraryGuid, form)
    }
  }

  def createWithConnection(createdBy: User, libraryGuid: UUID, form: VersionForm)(implicit c: java.sql.Connection): LibraryVersion = {
    val guid = UUID.randomUUID

    val sortKey = form.crossBuildVersion match {
      case None => Version(form.version).sortKey
      case Some(crossBuildVersion) => Version(s"${form.version}-$crossBuildVersion").sortKey
    }

    SQL(InsertQuery).on(
      'guid -> guid,
      'library_guid -> libraryGuid,
      'version -> form.version.trim,
      'cross_build_version -> form.crossBuildVersion.map(_.trim),
      'sort_key -> sortKey,
      'updated_by_user_id -> createdBy.id
    ).execute()

    findByGuidWithConnection(Authorization.All, guid).getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(deletedBy: User, guid: UUID) {
    SoftDelete.delete("library_versions", deletedBy.id, guid)
  }

  def findByLibraryAndVersionAndCrossBuildVersion(
    auth: Authorization,
    library: Library,
    version: String,
    crossBuildVersion: Option[String]
  ): Option[LibraryVersion] = {
    findAll(
      auth,
      libraryGuid = Some(library.guid),
      version = Some(version),
      crossBuildVersion = Some(crossBuildVersion),
      limit = 1
    ).headOption
  }

  def findByGuid(
    auth: Authorization,
    guid: UUID
  ): Option[LibraryVersion] = {
    DB.withConnection { implicit c =>
      findByGuidWithConnection(auth, guid)
    }
  }

  def findByGuidWithConnection(
    auth: Authorization,
    guid: UUID
  ) (
    implicit c: java.sql.Connection
  ): Option[LibraryVersion] = {
    findAllWithConnection(auth, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    libraryGuid: Option[UUID] = None,
    version: Option[String] = None,
    crossBuildVersion: Option[Option[String]] = None,
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
        libraryGuid = libraryGuid,
        version = version,
        crossBuildVersion = crossBuildVersion,
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
    libraryGuid: Option[UUID] = None,
    version: Option[String] = None,
    crossBuildVersion: Option[Option[String]] = None,
    greaterThanVersion: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("-library_versions.sort_key, library_versions.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ) (
    implicit c: java.sql.Connection
  ): Seq[LibraryVersion] = {
    Standards.query(
      BaseQuery,
      tableName = "library_versions",
      auth = auth.organizations("organizations.guid", Some("resolvers.visibility")),
      guid = guid,
      guids = guids,
      orderBy = orderBy.sql,
      isDeleted = isDeleted,
      limit = Some(limit),
      offset = offset
    ).
      equals("library_versions.library_guid", libraryGuid).
      text(
        "library_versions.version",
        version,
        columnFunctions = Seq(Query.Function.Lower),
        valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
      ).
      condition(
        crossBuildVersion.map { v =>
          v match {
            case None => s"library_versions.cross_build_version is null"
            case Some(_) => s"lower(library_versions.cross_build_version) = lower(trim({cross_build_version}))"
          }
        }
      ).
      bind(
        "cross_build_version",
        crossBuildVersion.flatMap { v => v }
      ).
      condition(
        greaterThanVersion.map { v =>
          s"library_versions.sort_key > {greater_than_version_sort_key}"
        }
      ).
      bind("greater_than_version_sort_key", greaterThanVersion).
      as(
        com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.table("library_versions").*
      )
  }

}

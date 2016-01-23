package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.api.lib.Version
import com.bryzek.dependency.v0.models.{Library, LibraryVersion, VersionForm}
import io.flow.postgresql.{Query, OrderBy}
import io.flow.common.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import scala.util.{Failure, Success, Try}

object LibraryVersionsDao {

  private[this] val BaseQuery = Query(s"""
    select library_versions.id,
           library_versions.version,
           library_versions.cross_build_version,
           libraries.id as library_id,
           libraries.group_id as library_group_id,
           libraries.artifact_id as library_artifact_id,
           organizations.id as library_organization_id,
           organizations.key as library_organization_key,
           resolvers.id as library_resolver_id,
           resolvers.visibility as library_resolver_visibility,
           resolvers.uri as library_resolver_uri,
           resolver_orgs.id as library_resolver_organization_id,
           resolver_orgs.key as library_resolver_organization_key
      from library_versions
      join libraries on libraries.deleted_at is null and libraries.id = library_versions.library_id
      join organizations on organizations.deleted_at is null and organizations.id = libraries.organization_id
      join resolvers on resolvers.deleted_at is null and resolvers.id = libraries.resolver_id
      left join organizations resolver_orgs on resolver_orgs.deleted_at is null and resolver_orgs.id = resolvers.organization_id
  """)

  private[this] val InsertQuery = s"""
    insert into library_versions
    (id, library_id, version, cross_build_version, sort_key, updated_by_user_id)
    values
    ({id}, {library_id}, {version}, {cross_build_version}, {sort_key}, {updated_by_user_id})
  """

  def upsert(createdBy: User, libraryId: String, form: VersionForm): LibraryVersion = {
    DB.withConnection { implicit c =>
      upsertWithConnection(createdBy, libraryId, form)
    }
  }

  private[db] def upsertWithConnection(createdBy: User, libraryId: String, form: VersionForm)(
    implicit c: java.sql.Connection
  ): LibraryVersion = {
    val auth = Authorization.User(createdBy.id)

    findAllWithConnection(
      auth,
      libraryId = Some(libraryId),
      version = Some(form.version),
      crossBuildVersion = Some(form.crossBuildVersion),
      limit = 1
    ).headOption.getOrElse {
      Try {
        createWithConnection(createdBy, libraryId, form)
      } match {
        case Success(version) => {
          version
        }
        case Failure(ex) => {
          // check concurrent insert
          findAllWithConnection(
            auth,
            libraryId = Some(libraryId),
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

  def create(createdBy: User, libraryId: String, form: VersionForm): LibraryVersion = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, libraryId, form)
    }
  }

  def createWithConnection(createdBy: User, libraryId: String, form: VersionForm)(implicit c: java.sql.Connection): LibraryVersion = {
    val id = io.flow.play.util.IdGenerator("liv").randomId()

    val sortKey = form.crossBuildVersion match {
      case None => Version(form.version).sortKey
      case Some(crossBuildVersion) => Version(s"${form.version}-$crossBuildVersion").sortKey
    }

    SQL(InsertQuery).on(
      'id -> id,
      'library_id -> libraryId,
      'version -> form.version.trim,
      'cross_build_version -> form.crossBuildVersion.map(_.trim),
      'sort_key -> sortKey,
      'updated_by_user_id -> createdBy.id
    ).execute()

    MainActor.ref ! MainActor.Messages.LibraryVersionCreated(id)

    findByIdWithConnection(Authorization.All, id).getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(deletedBy: User, id: String) {
    SoftDelete.delete("library_versions", deletedBy.id, id)
    MainActor.ref ! MainActor.Messages.LibraryVersionDeleted(id)
  }

  def findByLibraryAndVersionAndCrossBuildVersion(
    auth: Authorization,
    library: Library,
    version: String,
    crossBuildVersion: Option[String]
  ): Option[LibraryVersion] = {
    findAll(
      auth,
      libraryId = Some(library.id),
      version = Some(version),
      crossBuildVersion = Some(crossBuildVersion),
      limit = 1
    ).headOption
  }

  def findById(
    auth: Authorization,
    id: String
  ): Option[LibraryVersion] = {
    DB.withConnection { implicit c =>
      findByIdWithConnection(auth, id)
    }
  }

  def findByIdWithConnection(
    auth: Authorization,
    id: String
  ) (
    implicit c: java.sql.Connection
  ): Option[LibraryVersion] = {
    findAllWithConnection(auth, id = Some(id), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    libraryId: Option[String] = None,
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
        id = id,
        ids = ids,
        libraryId = libraryId,
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
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    libraryId: Option[String] = None,
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
      auth = auth.organizations("organizations.id", Some("resolvers.visibility")),
      id = id,
      ids = ids,
      orderBy = orderBy.sql,
      isDeleted = isDeleted,
      limit = limit,
      offset = offset
    ).
      equals("library_versions.library_id", libraryId).
      optionalText(
        "library_versions.version",
        version,
        columnFunctions = Seq(Query.Function.Lower),
        valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
      ).
      and(
        crossBuildVersion.map { v =>
          v match {
            case None => s"library_versions.cross_build_version is null"
            case Some(_) => s"lower(library_versions.cross_build_version) = lower(trim({cross_build_version}))"
          }
        }
      ).bind("cross_build_version", crossBuildVersion).
      and(
        greaterThanVersion.map { v =>
          s"library_versions.sort_key > {greater_than_version_sort_key}"
        }
      ).
      bind("greater_than_version_sort_key", greaterThanVersion).
      as(
        com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.parser().*
      )
  }

}

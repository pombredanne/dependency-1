package db

import com.bryzek.dependency.lib.VersionTag
import com.bryzek.dependency.v0.models.{Library, LibraryVersion, VersionForm}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import scala.util.{Failure, Success, Try}

object LibraryVersionsDao {

  private[this] val BaseQuery = s"""
    select library_versions.guid,
           library_versions.version,
           library_versions.cross_build_version,
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
    (guid, library_guid, version, cross_build_version, sort_key, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {library_guid}::uuid, {version}, {cross_build_version}, {sort_key}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, libraryGuid: UUID, form: VersionForm): LibraryVersion = {
    DB.withConnection { implicit c =>
      upsertWithConnection(createdBy, libraryGuid, form)
    }
  }

  private[db] def upsertWithConnection(createdBy: User, libraryGuid: UUID, form: VersionForm)(
    implicit c: java.sql.Connection
  ): LibraryVersion = {
    findAllWithConnection(
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

    val sortKey = Seq(
      Some(VersionTag(form.version).sortKey),
      form.crossBuildVersion.map(VersionTag(_).sortKey)
    ).flatten.mkString(VersionTag.Separator)

    SQL(InsertQuery).on(
      'guid -> guid,
      'library_guid -> libraryGuid,
      'version -> form.version.trim,
      'cross_build_version -> form.crossBuildVersion.map(_.trim),
      'sort_key -> sortKey,
      'created_by_guid -> createdBy.guid
    ).execute()

    findByGuidWithConnection(guid).getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(deletedBy: User, guid: UUID) {
    SoftDelete.delete("library_versions", deletedBy.guid, guid)
  }

  def findByLibraryAndVersionAndCrossBuildVersion(
    library: Library,
    version: String,
    crossBuildVersion: Option[String]
  ): Option[LibraryVersion] = {
    findAll(
      libraryGuid = Some(library.guid),
      version = Some(version),
      crossBuildVersion = Some(crossBuildVersion),
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
    libraryGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    version: Option[String] = None,
    crossBuildVersion: Option[Option[String]] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ) = {
    DB.withConnection { implicit c =>
      findAllWithConnection(
        guid = guid,
        guids = guids,
        libraryGuid = libraryGuid,
        projectGuid = projectGuid,
        version = version,
        crossBuildVersion = crossBuildVersion,
        isDeleted = isDeleted,
        limit = limit,
        offset = offset
      )
    }
  }

  def findAllWithConnection(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    libraryGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    version: Option[String] = None,
    crossBuildVersion: Option[Option[String]] = None,
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
      libraryGuid.map { v => s"and library_versions.library_guid = {library_guid}::uuid" },
      projectGuid.map { v => s"and library_versions.guid in (select library_version_guid from project_library_versions where deleted_at is null and project_guid = {project_guid}::uuid)" },
      version.map { v => s"and lower(library_versions.version) = lower(trim({version}))" },
      crossBuildVersion.map { v =>
        v match {
          case None => s"and library_versions.cross_build_version is null"
          case Some(_) => s"and lower(library_versions.cross_build_version) = lower(trim({cross_build_version}))"
        }
      },
      isDeleted.map(Filters.isDeleted("library_versions", _)),
      Some(s"order by library_versions.sort_key desc, library_versions.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      libraryGuid.map('library_guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      version.map('version -> _.toString),
      crossBuildVersion.flatMap { optionalValue =>
        optionalValue match {
          case None => None
          case Some(value) => Some('cross_build_version -> value.toString)
        }
      }
    ).flatten

    SQL(sql).on(bind: _*).as(
      com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.table("library_versions").*
    )
  }

}

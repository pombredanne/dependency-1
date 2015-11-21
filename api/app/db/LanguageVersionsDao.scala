package db

import com.bryzek.dependency.v0.models.{Language, LanguageVersion}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object LanguageVersionsDao {

  private[this] val BaseQuery = s"""
    select language_versions.guid,
           language_versions.version,
           ${AuditsDao.all("language_versions")},
           languages.guid as language_versions_language_guid,
           languages.name as language_versions_language_name,
           ${AuditsDao.all("languages", Some("language_versions_language"))}
      from language_versions
      join languages on languages.deleted_at is null and languages.guid = language_versions.language_guid
     where true
  """

  private[this] val InsertQuery = s"""
    insert into language_versions
    (guid, language_guid, version, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {language_guid}::uuid, {version}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, languageGuid: UUID, version: String): LanguageVersion = {
    DB.withConnection { implicit c =>
      upsertWithConnection(createdBy, languageGuid, version)
    }
  }

  private[db] def upsertWithConnection(createdBy: User, languageGuid: UUID, version: String)(
    implicit c: java.sql.Connection
  ): LanguageVersion = {
    findAllWithConnection(
      languageGuid = Some(languageGuid),
      version = Some(version),
      limit = 1
    ).headOption.getOrElse {
      createWithConnection(createdBy, languageGuid, version)
    }
  }

  def create(createdBy: User, languageGuid: UUID, version: String): LanguageVersion = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, languageGuid, version)
    }
  }

  def createWithConnection(createdBy: User, languageGuid: UUID, version: String)(implicit c: java.sql.Connection): LanguageVersion = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'language_guid -> languageGuid,
      'version -> version,
      'created_by_guid -> createdBy.guid
    ).execute()

    findByGuidWithConnection(guid).getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(deletedBy: User, guid: UUID) {
    SoftDelete.delete("language_versions", deletedBy.guid, guid)
  }

  def findByLanguageAndVersion(
    language: Language, version: String
  ) (
    implicit c: java.sql.Connection
  ): Option[LanguageVersion] = {
    findAllWithConnection(
      languageGuid = Some(language.guid),
      version = Some(version),
      limit = 1
    ).headOption
  }

  def findByGuid(
    guid: UUID
  ): Option[LanguageVersion] = {
    DB.withConnection { implicit c =>
      findByGuidWithConnection(guid)
    }
  }

  def findByGuidWithConnection(
    guid: UUID
  ) (
    implicit c: java.sql.Connection
  ): Option[LanguageVersion] = {
    findAllWithConnection(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    languageGuid: Option[UUID] = None,
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
        languageGuid = languageGuid,
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
    languageGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    version: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ) (
    implicit c: java.sql.Connection
  ): Seq[LanguageVersion] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => s"and language_versions.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids(s"language_versions.guid", _) },
      languageGuid.map { v => s"and language_versions.language_guid = {language_guid}::uuid" },
      projectGuid.map { v => s"and language_versions.guid in (select language_version_guid from project_language_versions where deleted_at is null and project_guid = {project_guid}::uuid)" },
      version.map { v => s"and lower(language_versions.version) = lower(trim({version}))" },
      isDeleted.map(Filters.isDeleted("language_versions", _)),
      Some(s"order by language_versions.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      languageGuid.map('language_guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      version.map('version -> _.toString)
    ).flatten

    SQL(sql).on(bind: _*).as(
      com.bryzek.dependency.v0.anorm.parsers.LanguageVersion.table("language_versions").*
    )
  }

}


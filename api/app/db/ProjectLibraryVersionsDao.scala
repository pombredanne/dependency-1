package db

import com.bryzek.dependency.v0.models.ProjectLibraryVersion
import io.flow.play.postgresql.AuditsDao
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object ProjectLibraryVersionsDao {

  private[this] val BaseQuery = s"""
    select library_versions.guid as library_version_guid,
           library_versions.version as library_version_version,
           ${AuditsDao.all("library_versions", Some("library_version"))},
           libraries.guid as library_version_library_guid,
           libraries.group_id as library_version_library_group_id,
           libraries.artifact_id as library_version_library_artifact_id,
           ${AuditsDao.all("libraries", Some("library_version_library"))},
           projects.guid as project_guid,
           projects.scms as project_scms,
           projects.name as project_name,
           projects.uri as project_uri,
           ${AuditsDao.all("projects", Some("project"))}
      from library_versions
      join libraries on libraries.deleted_at is null and libraries.guid = library_versions.library_guid
      join project_library_versions on project_library_versions.deleted_at is null and project_library_versions.library_version_guid = library_versions.guid
      join projects on projects.deleted_at is null and project_library_versions.project_guid = projects.guid
     where true
  """

  def findAll(
    projectGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    libraryGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    libraryVersionGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[ProjectLibraryVersion] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      projectGuid.map { v => "and projects.guid = {project_guid}::uuid" },
      libraryGuid.map { v => "and libraries.guid = {library_guid}::uuid" },
      libraryVersionGuid.map { v => "and library_versions.guid = {library_version_guid}::uuid" },
      Some(s"order by lower(projects.name), lower(libraries.group_id), lower(libraries.artifact_id), library_versions.sort_key limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      projectGuid.map('project_guid -> _.toString),
      libraryGuid.map('library_guid -> _.toString),
      libraryVersionGuid.map('library_version_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.ProjectLibraryVersion.parser(
          com.bryzek.dependency.v0.anorm.parsers.ProjectLibraryVersion.Mappings.base
        ).*
      )
    }
  }

}

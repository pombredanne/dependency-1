package db

import com.bryzek.dependency.v0.models.ProjectLanguageVersion
import io.flow.play.postgresql.AuditsDao
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object ProjectLanguageVersionsDao {

  private[this] val BaseQuery = s"""
    select language_versions.guid as language_version_guid,
           language_versions.version as language_version_version,
           ${AuditsDao.all("language_versions", Some("language_version"))},
           languages.guid as language_version_language_guid,
           languages.name as language_version_language_name,
           ${AuditsDao.all("languages", Some("language_version_language"))},
           projects.guid as project_guid,
           projects.scms as project_scms,
           projects.name as project_name,
           projects.uri as project_uri,
           ${AuditsDao.all("projects", Some("project"))}
      from language_versions
      join languages on languages.deleted_at is null and languages.guid = language_versions.language_guid
      join project_language_versions on project_language_versions.deleted_at is null and project_language_versions.language_version_guid = language_versions.guid
      join projects on projects.deleted_at is null and project_language_versions.project_guid = projects.guid
     where true
  """

  def findAll(
    projectGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    languageGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    languageVersionGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[ProjectLanguageVersion] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      projectGuid.map { v => "and projects.guid = {project_guid}::uuid" },
      languageGuid.map { v => "and languages.guid = {language_guid}::uuid" },
      languageVersionGuid.map { v => "and language_versions.guid = {language_version_guid}::uuid" },
      Some(s"order by lower(projects.name), lower(languages.name), language_versions.sort_key limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      projectGuid.map('project_guid -> _.toString),
      languageGuid.map('language_guid -> _.toString),
      languageVersionGuid.map('language_version_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.ProjectLanguageVersion.parser(
          com.bryzek.dependency.v0.anorm.parsers.ProjectLanguageVersion.Mappings.base
        ).*
      )
    }
  }

}

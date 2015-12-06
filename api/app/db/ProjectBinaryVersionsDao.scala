package db

import com.bryzek.dependency.v0.models.ProjectBinaryVersion
import io.flow.play.postgresql.AuditsDao
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object ProjectBinaryVersionsDao {

  private[this] val BaseQuery = s"""
    select binary_versions.guid as binary_version_guid,
           binary_versions.version as binary_version_version,
           ${AuditsDao.all("binary_versions", Some("binary_version"))},
           binaries.guid as binary_version_binary_guid,
           binaries.name as binary_version_binary_name,
           ${AuditsDao.all("binaries", Some("binary_version_binary"))},
           projects.guid as project_guid,
           projects.scms as project_scms,
           projects.visibility as project_visibility,
           projects.name as project_name,
           projects.uri as project_uri,
           ${AuditsDao.all("projects", Some("project"))}
      from binary_versions
      join binaries on binaries.deleted_at is null and binaries.guid = binary_versions.binary_guid
      join project_binary_versions on project_binary_versions.deleted_at is null and project_binary_versions.binary_version_guid = binary_versions.guid
      join projects on projects.deleted_at is null and project_binary_versions.project_guid = projects.guid
     where true
  """

  def findAll(
    projectGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    binaryGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    binaryVersionGuid: _root_.scala.Option[_root_.java.util.UUID] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[ProjectBinaryVersion] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      projectGuid.map { v => "and projects.guid = {project_guid}::uuid" },
      binaryGuid.map { v => "and binaries.guid = {binary_guid}::uuid" },
      binaryVersionGuid.map { v => "and binary_versions.guid = {binary_version_guid}::uuid" },
      Some(s"order by lower(projects.name), lower(binaries.name), binary_versions.sort_key limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      projectGuid.map('project_guid -> _.toString),
      binaryGuid.map('binary_guid -> _.toString),
      binaryVersionGuid.map('binary_version_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.ProjectBinaryVersion.parser(
          com.bryzek.dependency.v0.anorm.parsers.ProjectBinaryVersion.Mappings.base
        ).*
      )
    }
  }

}

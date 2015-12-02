package db

import com.bryzek.dependency.v0.models.{Recommendation, RecommendationType}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object RecommendationsDao {

  private[this] val BaseQuery = s"""
    select *
      from (select library_recommendations.guid,
                   library_recommendations.user_guid as recommendations_user_guid,
                   ${AuditsDao.all("library_recommendations")},
                   library_versions.guid as 
                   library_versions.version,
                   library_versions.cross_build_version,
                   ${AuditsDao.all("library_versions")},
                   libraries.guid as library_versions_library_guid,
                   libraries.group_id as library_versions_library_group_id,
                   libraries.artifact_id as library_versions_library_artifact_id,
                   ${AuditsDao.all("libraries", Some("library_versions_library"))},
                   '${RecommendationType.Library.toString}' as type
              from library_versions
              join libraries on libraries.deleted_at is null and libraries.guid = library_versions.library_guid
             where true)
     where true
  """

  def findByGuid(guid: UUID): Option[Recommendation] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    `type`: Option[RecommendationType] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Recommendation] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v =>  "and recommendations.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("recommendations.guid", _) },
      userGuid.map { v => "and recommendations.user_guid = {user_guid}::uuid" },
      `type`.map { v => "and recommendations.type = lower(trim({type}))" },
      isDeleted.map(Filters.isDeleted("recommendations", _)),
      Some(s"order by recommendations.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      `type`.map('type -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Recommendation.parser().*
      )
    }
  }

}

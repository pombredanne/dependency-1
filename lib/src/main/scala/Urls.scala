package com.bryzek.dependency.lib

import io.flow.play.util.DefaultConfig
import com.bryzek.dependency.v0.models.{BinarySummary, ItemSummary, ItemSummaryUndefinedType, LibrarySummary, ProjectSummary}
import com.bryzek.dependency.v0.models.{Recommendation, RecommendationType}
import java.util.UUID

/**
 * All our URLs to the webapp go here. We tried to use the www routers
 * directly as a separate project in the build, but caused problems in
 * the compile step (every other compile step failed). Instead we
 * provide hard coded urls - but keep in one file for easier
 * maintenance.
 */
case class Urls(
  wwwHost: String = DefaultConfig.requiredString("dependency.www.host")
) {

  val github = "https://github.com/mbryzek/dependency"

  def binary(guid: UUID) = s"/binaries/$guid"
  def library(guid: UUID) = s"/libraries/$guid"
  def project(guid: UUID) = s"/projects/$guid"

  def subscriptions(userIdentifier: Option[String]): String = {
    val base = "/subscriptions/"
    userIdentifier match {
      case None => base
      case Some(id) => {
        val encoded = play.utils.UriEncoding.encodePathSegment(id, "UTF-8")
        s"$base/?identifier=$encoded"
      }
    }
  }

  def www(rest: play.api.mvc.Call): String = {
    www(rest.toString)
  }

  def www(rest: String): String = {
    s"$wwwHost$rest"
  }


  def recommendation(recommendation: Recommendation): String = {
    recommendation.`type` match {
      case RecommendationType.Library => library(recommendation.`object`.guid)
      case RecommendationType.Binary => binary(recommendation.`object`.guid)
      case RecommendationType.UNDEFINED(_) => "#"
    }
  }

  def itemSummary(summary: ItemSummary): String = {
    summary match {
      case BinarySummary(guid, org, name) => binary(guid)
      case LibrarySummary(guid, org, groupId, artifactId) => library(guid)
      case ProjectSummary(guid, org, name) => project(guid)
      case ItemSummaryUndefinedType(name) => "#"
    }
  }

}

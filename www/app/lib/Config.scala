package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{BinarySummary, ItemSummary, ItemSummaryUndefinedType, LibrarySummary, ProjectSummary, Recommendation, RecommendationType}
import io.flow.play.util.DefaultConfig
import java.net.URLEncoder

object Config {

  lazy val githubClientId = DefaultConfig.requiredString("github.dependency.client.id")
  lazy val dependencyWwwHost = DefaultConfig.requiredString("dependency.www.host")
  lazy val githubBaseUrl = s"$dependencyWwwHost/login/github"

  private val GithubScopes = Seq("user:email", "repo")

  private[this] val GitHubOauthUrl = "https://github.com/login/oauth/authorize"

  def githubOauthUrl(returnUrl: Option[String]): String = {
    GitHubOauthUrl + "?" + Seq(
      Some("scope" -> GithubScopes.mkString(",")),
      Some("client_id" -> githubClientId),
      returnUrl.map { url => ("redirect_uri" -> (s"$githubBaseUrl?return_url=" + URLEncoder.encode(url, "UTF-8"))) }
    ).flatten.map { case (key, value) =>
        s"$key=" + URLEncoder.encode(value, "UTF-8")
    }.mkString("&")

  }

  def recommendationUrl(recommendation: Recommendation): String = {
    recommendation.`type` match {
      case RecommendationType.Library => controllers.routes.LibrariesController.show(recommendation.`object`.guid).url
      case RecommendationType.Binary => controllers.routes.BinariesController.show(recommendation.`object`.guid).url
      case RecommendationType.UNDEFINED(_) => "#"
    }
  }

  def itemSummaryUrl(summary: ItemSummary): String = {
    summary match {
      case BinarySummary(guid, _) => controllers.routes.BinariesController.show(guid).url
      case LibrarySummary(guid, _, _) => controllers.routes.LibrariesController.show(guid).url
      case ProjectSummary(guid, _) => controllers.routes.ProjectsController.show(guid).url
      case ItemSummaryUndefinedType(_) => "#"
    }
  }

}


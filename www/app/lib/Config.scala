package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{Recommendation, RecommendationType}
import io.flow.play.util.DefaultConfig
import java.net.URLEncoder

object Config {

  lazy val githubClientId = DefaultConfig.requiredString("github.dependency.client.id")
  lazy val dependencyWwwHost = DefaultConfig.requiredString("dependency.www.host")
  lazy val githubBaseUrl = s"$dependencyWwwHost/login/github"

  private[this] val GitHubOauthUrl = "https://github.com/login/oauth/authorize"

  def githubOauthUrl(returnUrl: Option[String]): String = {
    GitHubOauthUrl + "?" + Seq(
      Some("scope" -> "user:email,repo"),
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

}


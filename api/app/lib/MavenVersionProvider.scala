package com.bryzek.dependency.lib

import io.flow.play.util.Config
import io.flow.maven.v0.Client

import play.api.Logger
import scala.concurrent.Future

object MavenVersionProvider extends VersionProvider {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val client = new Client(Config.requiredString("maven.api.url"))

  /**
    * Fetches the latest version of a an artifact on maven.
    * 
    * Example:
    *   latestVersion("com.google.inject", "guice").map { 
    *     ...
    *   }
    */
  def latestVersion(
    groupId: String,
    artifactId: String
  ): Future[Option[String]] = {
    client.docs.getSelect(s"g:$groupId AND a:$artifactId").map { apiResponse =>
      apiResponse.response.docs.map(_.latestVersion) match {
        case Nil => None
        case version :: Nil => Some(version)
        case multiple => {
          Logger.warn(s"Multiple versions found for group[$groupId] artifact[$artifactId]")
          multiple.headOption
        }
      }
    }
  }
}

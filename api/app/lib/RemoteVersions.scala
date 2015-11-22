package com.bryzek.dependency.lib

import org.apache.commons.lang3.StringUtils
import java.net.URL
import scala.util.{Failure, Success, Try}
import play.api.Logger

object RemoteVersions {

  def fetch(
    resolver: String,
    groupId: String,
    artifactId: String
  ): Seq[ArtifactVersion] = {
    fetchUrl(
      url = makeUrl(resolver, groupId),
      filter = { name => name == artifactId || name.startsWith(artifactId + "_") }
    ).sortBy { _.tag }
  }

  private[this] def fetchUrl(
    url: String,
    filter: String => Boolean
  ): Seq[ArtifactVersion] = {
    val result = RemoteDirectory.fetch(url)(filter)

    result.directories.flatMap { dir =>
      val thisUrl = joinUrl(url, dir)
      RemoteDirectory.fetch(thisUrl)().directories.map { d =>
        ArtifactVersion(
          tag = VersionTag(StringUtils.stripEnd(d, "/")),
          crossBuildVersion = crossBuildVersion(dir)
        )
      }
    }
  }

  // e.g. "scala-csv_2.11/" => 2.11
  def crossBuildVersion(text: String): Option[VersionTag] = {
    StringUtils.stripEnd(text, "/").split("_").toList match {
      case Nil => None
      case one :: Nil => None
      case multiple => {
        // Check if we can successfully parse the version tag for a
        // major version. If so, we assume we have found a cross build
        // version.
        val tag = VersionTag(multiple.last)
        tag.major match {
          case None => None
          case Some(_) => Some(tag)
        }
      }
    }
  }

  def makeUrl(
    resolver: String,
    groupId: String
  ): String = {
    joinUrl(resolver, groupId.replaceAll("\\.", "/"))
  }

  def joinUrl(
    a: String,
    b: String
  ): String = {
    Seq(a, b).map ( StringUtils.stripEnd(_, "/") ).mkString("/")
  }
}

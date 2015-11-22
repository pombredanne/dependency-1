package com.bryzek.dependency.lib

import org.htmlcleaner.HtmlCleaner
import org.apache.commons.lang3.{StringEscapeUtils, StringUtils}
import java.net.URL
import scala.util.{Failure, Success, Try}
import play.api.Logger

object RemoteVersions {

  case class Version(
    tag: VersionTag,
    crossBuildVersion: Option[VersionTag]
  )

  def fetch(
    resolver: String,
    groupId: String,
    artifactId: String
  ): Seq[Version] = {
    fetchUrl(
      url = makeUrl(resolver, groupId),
      filter = { name => name == artifactId || name.startsWith(artifactId + "_") }
    ).sortBy { _.tag }
  }

  private[this] def fetchUrl(
    url: String,
    filter: String => Boolean
  ): Seq[Version] = {
    val result = RemoteDirectory.fetch(url)(filter)

    result.directories.flatMap { dir =>
      val thisUrl = joinUrl(url, dir)
      RemoteDirectory.fetch(thisUrl)().directories.map { d =>
        fetchVersionFromMavenMetadata(
          joinUrl(thisUrl, d),
          crossBuildVersion = crossBuildVersion(dir)
        )
      }
    }.flatten
  }

  def fetchVersionFromMavenMetadata(
    url: String,
    crossBuildVersion: Option[VersionTag] = None
  ): Option[Version] = {
    val fullUrl = Seq(url, "maven-metadata.xml").mkString("/")

    val cleaner = new HtmlCleaner()
    Try(cleaner.clean(new URL(fullUrl))) match {
      case Failure(ex) => ex match {
        case e: java.io.FileNotFoundException => None
        case _ => {
          Logger.error("Error fetching URL[$fullUrl]: $ex")
          None
        }
      }
      case Success(rootNode) => {
        rootNode.getElementsByName("version", true).headOption.flatMap { version =>
          StringEscapeUtils.unescapeHtml4(version.getText.toString).trim match {
            case "" => {
              None
            }
            case name => Some(
              Version(
                tag = VersionTag(name),
                crossBuildVersion = crossBuildVersion
              )
            )
          }
        }.headOption
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

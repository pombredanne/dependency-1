package com.bryzek.dependency.lib

import org.htmlcleaner.HtmlCleaner
import org.apache.commons.lang3.{StringEscapeUtils, StringUtils}
import java.net.URL
import scala.util.{Failure, Success, Try}
import play.api.Logger

object RemoteDirectory {

  case class Version(
    name: String, // e.g. 1.2.3
    crossBuildVersion: Option[String]
  )

  case class Result(
    url: String,
    files: Seq[String] = Nil,
    versions: Seq[Version] = Nil
  )

  def fetch(
    resolver: String,
    groupId: String,
    artifactId: String
  ) (
    filter: String => Boolean = { name => name == artifactId || name.startsWith(artifactId + "_") }
  ): Result = {
    val result = Result(url = makeUrl(resolver, groupId))
    fetchUri(result, filter)
  }

  private[this] def fetchUri(
    result: Result,
    filter: String => Boolean
  ): Result = {
    val base = Result(url = result.url)
    println(s"==> Fetching ${base.url}")

    val cleaner = new HtmlCleaner()
    Try(cleaner.clean(new URL(base.url))) match {
      case Failure(ex) => ex match {
        case e: java.io.FileNotFoundException => {
          base
        }
        case _ => {
          Logger.error("Error fetching URL[$url]: $ex")
          base
        }
      }
      case Success(rootNode) => {
        rootNode.getElementsByName("a", true).foldLeft(base) { case (result, elem) =>
          Option(elem.getAttributeByName("href")) match {
            case None => result
            case Some(rawHref) => {
              val text = StringEscapeUtils.unescapeHtml4(elem.getText.toString)
              val href =StringEscapeUtils.unescapeHtml4(rawHref)
              filter(text) match {
                case false => {
                  result
                }
                case true => {
                  text.endsWith("/") match {
                    case true => result.copy(
                      versions = result.versions ++ Seq(
                        fetchVersionFromMavenMetadata(
                          joinUrl(result.url, text),
                          crossBuildVersion = crossBuildVersion(text)
                        )
                      ).flatten
                    )
                    case false => result.copy(files = result.files ++ Seq(text))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def fetchVersionFromMavenMetadata(
    url: String,
    crossBuildVersion: Option[String] = None
  ): Option[Version] = {
    val fullUrl = Seq(url, "maven-metadata.xml").mkString("/")
    println("FULL URL: " + fullUrl)

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
        val allVersions: Seq[Version] = rootNode.getElementsByName("metadata", true).flatMap { metadata =>
          metadata.getElementsByName("versioning", true).flatMap { versioning =>
            versioning.getElementsByName("versions", true).flatMap { versions =>
              versions.getElementsByName("version", true).flatMap { version =>
                StringEscapeUtils.unescapeHtml4(version.getText.toString).trim match {
                  case "" => None
                  case name => Some(Version(name = name, crossBuildVersion = crossBuildVersion))
                }
              }
            }
          }
        }

        allVersions.distinct.toList match {
          case Nil => {
            Logger.warn("No versions found for URL[$fullUrl]")
            None
          }
          case one :: Nil => Some(one)
          case multiple => {
            Logger.warn("Multiple versions found for URL[$fullUrl] - returning first version")
            multiple.headOption
          }
        }
      }
    }
  }

  // e.g. "scala-csv_2.11/" => 2.11
  def crossBuildVersion(text: String): Option[String] = {
    StringUtils.stripEnd(text, "/").split("_").toList match {
      case Nil => None
      case one :: Nil => None
      case multiple => {
        // Check if we can successfully parse the version tag for a
        // major version. If so, we assume we have found a cross build
        // version.
        VersionTag(multiple.last).major match {
          case None => None
          case Some(_) => Some(multiple.last)
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

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

  def fetch(
    resolver: String,
    groupId: String,
    artifactId: String
  ): Seq[Version] = {
    fetchUrl(
      url = makeUrl(resolver, groupId),
      filter = { name => name == artifactId || name.startsWith(artifactId + "_") }
    )
  }

  private[this] def fetchUrl(
    url: String,
    filter: String => Boolean
  ): Seq[Version] = {
    val cleaner = new HtmlCleaner()
    Try(cleaner.clean(new URL(url))) match {
      case Failure(ex) => ex match {
        case e: java.io.FileNotFoundException => {
          Nil
        }
        case _ => {
          Logger.error("Error fetching URL[$url]: $ex")
          Nil
        }
      }
      case Success(rootNode) => {
        rootNode.getElementsByName("a", true).foldLeft(Seq[Version]()) { case (versions, elem) =>
          Option(elem.getAttributeByName("href")) match {
            case None => {
              versions
            }
            case Some(rawHref) => {
              val text = StringEscapeUtils.unescapeHtml4(elem.getText.toString)
              val href = StringEscapeUtils.unescapeHtml4(rawHref)
              filter(text) match {
                case false => {
                  versions
                }
                case true => {
                  text.endsWith("/") match {
                    case true => {
                      versions ++ Seq(
                        fetchVersionFromMavenMetadata(
                          joinUrl(url, text),
                          crossBuildVersion = crossBuildVersion(text)
                        )
                      ).flatten
                    }
                    case false => {
                      println("Got a basic version: $text")
                      versions ++ Seq(Version(name = text, crossBuildVersion = None))
                    }
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

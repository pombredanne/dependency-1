package com.bryzek.dependency.lib

import scala.util.{Failure, Success, Try}
import play.api.Logger

case class SearchQuery(
  groupId: Option[String] = None,
  artifactId: Option[String] = None,
  version: Option[String] = None
) {

  val query: String = {
    Map(
      "groupId" -> groupId,
      "artifactId" -> artifactId,
      "version" -> version
    ).filter { case (key, value) => !value.map(_.trim).getOrElse("").isEmpty }.
      map { case (key, value) => s"$key=${value.get.trim}" }.
      mkString(" ")
  }

}

object SearchQuery {

  def parse(value: String): SearchQuery = {
    var q = SearchQuery()
    value.replaceAll("\\s+", " ").replaceAll("\\s*=\\s*", "=").trim match {
      case "" => {
        // No-op
      }
      case v => {
        v.split("\\s+").foreach { piece =>
          piece.split("=").toList match {
            case key :: value :: Nil => {
              key.toLowerCase match {
                case "groupid" => q = q.copy(groupId = Some(value))
                case "artifactid" => q = q.copy(artifactId = Some(value))
                case "version" => q = q.copy(version = Some(value))
                case _ => {
                  Logger.warn(s"Invalid query[$q] component[$v] contained unrecognized key[$key]")
                }
              }
            }
            case other => {
              Logger.warn(s"Invalid query[$q] component[$v] did not contain two parts")
            }
          }
        }
      }
    }
    q
  }

}

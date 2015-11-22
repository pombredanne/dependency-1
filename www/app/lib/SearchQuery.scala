package com.bryzek.dependency.lib

import scala.util.{Failure, Success, Try}

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
    ).filter { case (key, value) => !value.isEmpty }.
      map { case (key, value) => s"$key='${value.get}'" }.
      mkString(" and ")
  }

}

object SearchQuery {

  def parse(value: String): SearchQuery = {
    value.trim match {
      case "" => SearchQuery()
      case v => SearchQueryParser.parse(v) match {
        case Left(errors) => sys.error("Error parsing query[$value]: " + errors.mkString(", "))
        case Right(query) => query
      }
    }
  }

}

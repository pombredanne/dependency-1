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
    ).filter { case (key, value) => !value.map(_.trim).getOrElse("").isEmpty }.
      map { case (key, value) => s"$key=${value.get.trim}" }.
      mkString(" and ")
  }

}

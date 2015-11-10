package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{LanguageForm, LibraryForm}

/**
  * Takes the contents of a build.sbt file and parses it, providing
  * access to its dependencies (libraries, languages and versions).
  */
case class ParseBuildSbt(contents: String) {

  private val Scala = "scala"

  private val lines = contents.
    split("\n").
    map(_.trim).
    filter(!_.isEmpty).
    filter(!_.startsWith("//"))

  val languages: Seq[LanguageForm] = {
    lines.
      filter(_.startsWith("scalaVersion")).
      flatMap { line =>
      line.split(":=").map(_.trim).toList match {
        case head :: Nil => {
          Some(
            LanguageForm(
              name = head
            )
          )
        }
        case head :: version :: Nil => {
          Some(
            LanguageForm(
              name = head,
              version = Some(stripQuotes(version))
            )
          )
        }
        case _ => {
          None
        }
      }
    }
  }.distinct.sortBy { l => s"${l.name}:${l.version}" }

  val libraries: Seq[LibraryForm] = {
    lines.
      filter(_.replaceAll("%%", "%").split("%").size >= 2).
      map(stripComments(_)).
      map(_.trim).
      map(_.stripSuffix(",")).
      map(_.trim).
      map { line =>
        toLibraryForm(line) match {
          case Left(error) => sys.error(error)
          case Right(library) => library
        }
      }
  }.distinct.sortBy { l => s"${l.groupId}:${l.artifactId}:${l.version}" }

  def toLibraryForm(value: String): Either[String, LibraryForm] = {
    value.replaceAll("%%", "%").split("%").map(_.trim).toList match {
      case Nil => {
        Left(s"Could not parse library from[$value]")
      }
      case groupId :: Nil => {
        Left(s"Could not parse library from[$value] - only found groupId[$groupId]")
      }
      case groupId :: artifactId :: Nil => {
        Right(
          LibraryForm(
            groupId = stripQuotes(groupId),
            artifactId = stripQuotes(artifactId)
          )
        )
      }
      case groupId :: artifactId :: version :: more => {
        Right(
          LibraryForm(
            groupId = stripQuotes(groupId),
            artifactId = stripQuotes(artifactId),
            version = Some(stripQuotes(version))
          )
        )
      }
    }
  }

  def stripQuotes(value: String): String = {
    value.stripPrefix("\"").stripSuffix("\"").trim
  }

  def stripComments(value: String): String = {
    val i = value.indexOf("//")
    if (i < 0) {
      value
    } else {
      value.substring(0, i)
    }
  }

}

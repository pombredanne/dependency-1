package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{Language, Library}

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

  val languages: Seq[Language] = {
    lines.
      filter(_.startsWith("scalaVersion")).
      flatMap { line =>
      line.split(":=").map(_.trim).toList match {
        case head :: version :: Nil => {
          Some(Language(Scala, stripQuotes(version)))
        }
        case _ => {
          None
        }
      }
    }
  }

  val libraries: Seq[Library] = {
    lines.
      filter(_.replaceAll("%%", "%").split("%").size >= 2).
      map(stripComments(_)).
      map(_.trim).
      map(_.stripSuffix(",")).
      map(_.trim).
      map { line =>
        toLibrary(line) match {
          case Left(error) => sys.error(error)
          case Right(library) => library
        }
      }
  }

  def toLibrary(value: String): Either[String, Library] = {
    value.replaceAll("%%", "%").split("%").map(_.trim).toList match {
      case Nil => {
        Left(s"Could not parse library from[$value]")
      }
      case groupId :: Nil => {
        Left(s"Could not parse library from[$value] - only found groupId[$groupId]")
      }
      case groupId :: artifactId :: Nil => {
        Left(s"Could not parse library from[$value] - missing version for groupId[$groupId] artifactId[$artifactId]")
      }
      case groupId :: artifactId :: version :: more => {
        Right(Library(stripQuotes(groupId), stripQuotes(artifactId), stripQuotes(version)))
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

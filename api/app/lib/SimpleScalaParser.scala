package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.LibraryForm

trait SimpleScalaParser {

  def contents: String

  case class Variable(name: String, value: String)

  lazy val lines = parseIntoLines(contents)

  /**
    * Parses into meaningful lines of data, stripping comments and
    * removing blank lines
    */
  def parseIntoLines(contents: String): Seq[String] = {
    contents.
      split("\n").
      map(stripComments(_)).
      map(_.trim).
      filter(!_.isEmpty)
  }

  // Pull out all lines that start w/ "val " or "var " and capture
  // variable declarations
  lazy val variables: Seq[Variable] = lines.
    filter(line => line.startsWith("val ") || line.startsWith("var ")).
    flatMap { line =>
      line.split("=").map(_.trim).toList match {
        case declaration :: value :: Nil => {
          Some(Variable(declaration.substring(declaration.indexOf(" ")).trim, stripQuotes(value)))
        }
        case _ => {
          None
        }
      }
    }

  def interpolate(value: String): String = {
    val formatted = stripQuotes(value)
    variables.find(_.name == formatted) match {
      case None => {
        formatted
      }
      case Some(variable) => variable.value
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
            groupId = interpolate(groupId),
            artifactId = interpolate(artifactId)
          )
        )
      }
      case groupId :: artifactId :: version :: more => {
        Right(
          LibraryForm(
            groupId = interpolate(groupId),
            artifactId = interpolate(artifactId),
            version = Some(interpolate(version))
          )
        )
      }
    }
  }

}

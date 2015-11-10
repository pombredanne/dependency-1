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

  /**
    * This method will substitute any variables for their values. For
    * literals, we strip the quotes.
    */
  def interpolate(value: String): String = {
    variables.find(_.name == value) match {
      case None => stripQuotes(value)
      case Some(variable) => variable.value
    }
  }

  /**
   * Removes leading and trailing quotes
   */
  def stripQuotes(value: String): String = {
    value.stripPrefix("\"").stripSuffix("\"").trim
  }

  /**
    * Removes any in-line comments - handles both block and trailing // comments.
    * 
    * Taken from http://stackoverflow.com/questions/1657066/java-regular-expression-finding-comments-in-code
    */
  def stripComments(value: String): String = {
    value.replaceAll( "//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "$1 " ).trim
  }


  def parseLibraries(): Seq[LibraryForm] = {
    lines.
      filter(_.replaceAll("%%", "%").split("%").size >= 2).
      map(_.stripSuffix(",")).
      map(_.trim).
      map { line =>
        toLibraryForm(line) match {
          case Left(error) => sys.error(error)
          case Right(library) => library
        }
      }.distinct.sortBy { l => (l.groupId, l.artifactId, l.version) }
  }

  def toLibraryForm(value: String): Either[String, LibraryForm] = {
    val firstParen = value.indexOf("(")
    val lastParen = value.lastIndexOf(")")

    val substring = if (firstParen >= 0) {
      value.substring(firstParen+1, lastParen)
    } else {
      value
    }

    substring.replaceAll("%%", "%").split("%").map(_.trim).toList match {
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

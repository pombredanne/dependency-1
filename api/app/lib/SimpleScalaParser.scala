package com.bryzek.dependency.lib

import play.api.Logger
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
    filter(SimpleScalaParserUtil.definesVariable(_)).
    flatMap { line =>
      line.split("=").map(_.trim).toList match {
        case declaration :: value :: Nil => {
          Some(Variable(declaration.substring(declaration.indexOf(" ")).trim, cleanVariable(value)))
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
      case None => cleanVariable(value)
      case Some(variable) => variable.value
    }
  }

  @scala.annotation.tailrec
  final def cleanVariable(value: String): String = {
    val stripped = stripQuotes(value)
    (stripped == value) match {
      case false => cleanVariable(stripped)
      case true => {
        val stripped2 = stripTrailingCommas(value)
          (stripped2 == value) match {
          case false => cleanVariable(stripped2)
          case true => value
        }
      }
    }
  }

  /**
   * Removes leading and trailing quotes
   */
  def stripQuotes(value: String): String = {
    value.stripPrefix("\"").stripSuffix("\"").trim
  }

  /**
   * Removes leading and trailing quotes
   */
  def stripTrailingCommas(value: String): String = {
    value.stripSuffix(",").trim
  }

  /**
    * Removes any in-line comments - handles both block and trailing // comments.
    * 
    * Taken from http://stackoverflow.com/questions/1657066/java-regular-expression-finding-comments-in-code
    */
  def stripComments(value: String): String = {
    value.replaceAll( "//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "$1 " ).trim
  }


  def parseLibraries(): Seq[Artifact] = {
    lines.
      filter(_.replaceAll("%%", "%").split("%").size >= 2).
      map(_.stripSuffix(",")).
      map(_.trim).
      flatMap { line =>
        toArtifact(line) match {
          case Left(error) => {
            Logger.warn(error)
            None
          }
          case Right(library) => Some(library)
        }
      }.distinct.sortBy { l => (l.groupId, l.artifactId, l.version) }
  }

  def toArtifact(value: String): Either[String, Artifact] = {
    val firstParen = value.indexOf("(")
    val lastParen = value.lastIndexOf(")")

    val substring = if (firstParen >= 0) {
      value.substring(firstParen+1, lastParen)
    } else {
      value
    }

    val isCrossBuilt = substring.indexOf("%%") >= 0

    substring.replaceAll("%%", "%").split("%").map(_.trim).toList match {
      case Nil => {
        Left(s"Could not parse library from[$value]")
      }
      case groupId :: Nil => {
        Left(s"Could not parse library from[$value] - only found groupId[$groupId] but missing artifactId and version")
      }
      case groupId :: artifactId :: Nil => {
        Left(s"Could not parse library from[$value] - only found groupId[$groupId] and artifactId[$artifactId] but missing version")
      }
      case groupId :: artifactId :: version :: more => {
        Right(
          Artifact(
            groupId = interpolate(groupId),
            artifactId = interpolate(artifactId),
            version = interpolate(version),
            isCrossBuilt = isCrossBuilt
          )
        )
      }
    }
  }

}

object SimpleScalaParserUtil {

  @scala.annotation.tailrec
  def definesVariable(line: String): Boolean = {
    val trimmed = line.trim
    (trimmed.startsWith("val ") || trimmed.startsWith("var ")) match {
      case true => true
      case false => {
        trimmed.startsWith("lazy ") match {
          case false => false
          case true => {
            definesVariable(trimmed.substring(5))
          }
        }
      }
    }
  }

}

package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{Language, Library}

/**
  * Takes the contents of a build.sbt file and parses it, providing
  * access to its dependencies (libraries, languages and versions).
  */
case class ParseBuildSbt(contents: String) {

  private val Scala = "scala"

  private val lines = contents.split("\n").map(_.trim).filter(!_.isEmpty)

  val languages: Seq[Language] = {
    lines.
      filter(_.startsWith("scalaVersion")).
      filter(!_.startsWith("//")).
      flatMap { line =>
      line.split(":=").map(_.trim).toList match {
        case head :: tail :: Nil => {
          stripQuotes(tail).map { version => Language(Scala, version) }
        }
        case _ => {
          None
        }
      }
    }
  }

  val libraries: Seq[Library] = {
    // lines.filter(_.startsWith("scalaVersion")).flatMap { line =>
    Nil
  }

  def stripQuotes(value: String): Option[String] = {
    value.stripPrefix("\"").stripSuffix("\"").trim match {
      case "" => None
      case some => Some(some)
    }
  }

}

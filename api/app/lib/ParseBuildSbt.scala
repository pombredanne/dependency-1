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
    lines.find(_.startsWith("scalaVersion")) match {
      case None => Nil
      case Some(line) => {
        line.split(":=").map(_.trim).toList match {
          case Nil => {
            Nil
          }
          case head :: tail :: Nil => {
            stripQuotes(tail) match {
              case None => Nil
              case Some(version) => Seq(Language(Scala, version))
            }
          }
          case _ => {
            Nil
          }
        }
      }
    }
  }

  val libraries: Seq[Library] = {
    Nil
  }

  def stripQuotes(value: String): Option[String] = {
    value.stripPrefix("\"").stripSuffix("\"").trim match {
      case "" => None
      case some => Some(some)
    }
  }

}

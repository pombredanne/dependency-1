package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.LanguageForm

/**
  * Takes the contents of a build.sbt file and parses it, providing
  * access to its dependencies (libraries, languages and versions).
  */
case class BuildSbtScalaParser(
  override val description: String,
  contents: String
) extends SimpleScalaParser {

  private val LanguageScala = "scala"

  private lazy val pluginParser = ProjectPluginsSbtScalaParser(description, contents)

  val resolverUris = pluginParser.resolverUris

  val libraries: Seq[Artifact] = parseLibraries

  val languages: Seq[LanguageForm] = {
    lines.
      filter(_.startsWith("scalaVersion")).
      flatMap { line =>
      line.split(":=").map(_.trim).toList match {
        case head :: version :: Nil => {
          Some(
            LanguageForm(
              name = LanguageScala,
              version = interpolate(version)
            )
          )
        }
        case _ => {
          None
        }
      }
    }
  }.distinct.sortBy { l => (l.name, l.version) }

}

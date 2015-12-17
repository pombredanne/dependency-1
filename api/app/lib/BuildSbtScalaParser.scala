package com.bryzek.dependency.api.lib

import com.bryzek.dependency.v0.models.{BinaryForm, ProjectSummary}

/**
  * Takes the contents of a build.sbt file and parses it, providing
  * access to its dependencies (libraries, binaries and versions).
  */
case class BuildSbtScalaParser(
  override val project: ProjectSummary,
  override val path: String,
  contents: String
) extends SimpleScalaParser {

  private val BinaryScala = "scala"

  private lazy val pluginParser = ProjectPluginsSbtScalaParser(project, path, contents)

  val resolverUris = pluginParser.resolverUris

  val libraries: Seq[Artifact] = parseLibraries

  val binaries: Seq[BinaryForm] = {
    lines.
      filter(_.startsWith("scalaVersion")).
      flatMap { line =>
      line.split(":=").map(_.trim).toList match {
        case head :: version :: Nil => {
          Some(
            BinaryForm(
              organizationGuid = project.organization.guid,
              name = BinaryScala,
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

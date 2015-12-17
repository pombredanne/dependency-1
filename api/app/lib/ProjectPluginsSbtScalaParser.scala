package com.bryzek.dependency.api.lib

import com.bryzek.dependency.v0.models.ProjectSummary

/**
  * Takes the contents of a project/plugins.sbt file and parses it, providing
  * access to its dependencies (repositories and plugins).
  */
case class ProjectPluginsSbtScalaParser(
  override val project: ProjectSummary,
  override val path: String,
  contents: String
) extends SimpleScalaParser {

  val plugins: Seq[Artifact] = parseLibraries

  val resolverUris: Seq[String] = {
    lines.
      filter(_.startsWith("resolvers ")).
      filter(_.indexOf(" at ") > 0).
      map { line =>
        interpolate(line.substring(line.indexOf(" at ") + 3).trim)
      }.distinct.sortBy(_.toLowerCase)
  }

}

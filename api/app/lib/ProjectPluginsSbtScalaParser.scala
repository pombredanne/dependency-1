package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{LanguageForm, LibraryForm}

/**
  * Takes the contents of a project/plugins.sbt file and parses it, providing
  * access to its dependencies (repositories and plugins).
  */
case class ProjectPluginsSbtScalaParser(contents: String) extends SimpleScalaParser {

  val plugins: Seq[LibraryForm] = parseLibraries

  val resolvers: Seq[Resolver] = {
    lines.
      filter(_.startsWith("resolvers ")).
      map { line =>
        Resolver(interpolate(line.substring(line.indexOf(" at ") + 3).trim))
      }.distinct.sortBy(_.uri)
  }

}

package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{LanguageForm, LibraryForm}
import org.specs2.mutable._

class ProjectPluginsSbtScalaParserSpec extends Specification {

  "empty" should {

    val contents = """
// Comment to get more information during initialization
logLevel := Level.Warn
"""

    "parse dependencies" in {
      val result = ProjectPluginsSbtScalaParser(contents)
      result.resolvers must beEqualTo(Nil)
      result.plugins must beEqualTo(Nil)
    }

  }

  "with resolver" should {

    val contents = """
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
"""

    "parse dependencies" in {
      val result = ProjectPluginsSbtScalaParser(contents)
      result.resolvers must beEqualTo(Seq(Resolver("http://repo.typesafe.com/typesafe/releases/")))
      result.plugins must beEqualTo(Nil)
    }

  }

  "with resolver and plugins" should {

    val contents = """
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.3")

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.0.1")
"""

    "parse dependencies" in {
      val result = ProjectPluginsSbtScalaParser(contents)
      result.resolvers must beEqualTo(Seq(Resolver("http://repo.typesafe.com/typesafe/releases/")))
      result.plugins must beEqualTo(
        Seq(
          LibraryForm("com.typesafe.play", "sbt-plugin", Some("2.4.3")),
          LibraryForm("org.scoverage", "sbt-scoverage", Some("1.0.1"))
        )
      )
    }

  }

}

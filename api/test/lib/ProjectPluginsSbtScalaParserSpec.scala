package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.LanguageForm
import org.specs2.mutable._

class ProjectPluginsSbtScalaParserSpec extends Specification {

  "empty" should {

    val contents = """
// Comment to get more information during initialization
logLevel := Level.Warn
"""

    "parse dependencies" in {
      val result = ProjectPluginsSbtScalaParser(contents)
      result.resolverUris must beEqualTo(Nil)
      result.plugins must beEqualTo(Nil)
    }

  }

  "with resolver" should {

    val contents = """
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
"""

    "parse dependencies" in {
      val result = ProjectPluginsSbtScalaParser(contents)
      result.resolverUris must beEqualTo(Seq("http://repo.typesafe.com/typesafe/releases/"))
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
      result.resolverUris must beEqualTo(
        Seq("http://repo.typesafe.com/typesafe/releases/")
      )
      result.plugins must beEqualTo(
        Seq(
          Artifact("com.typesafe.play", "sbt-plugin", "2.4.3", false),
          Artifact("org.scoverage", "sbt-scoverage", "1.0.1", true)
        )
      )
    }

  }

  "with inline resolvers" should {

    val contents = """
lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
  organization := "com.cavellc",
  name <<= name("cave-" + _),
  version := "git describe --tags --dirty --always".!!.stripPrefix("v").trim,
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  libraryDependencies ++= Seq(
    "io.dropwizard.metrics" % "metrics-core" % "3.1.0",
    "io.dropwizard.metrics" % "metrics-jvm" % "3.1.0",
    "org.scalatest" %% "scalatest" % "2.1.2" % "test"
  )
)
"""

    "parse dependencies" in {
      val result = ProjectPluginsSbtScalaParser(contents)
      result.resolverUris must beEqualTo(
        Seq("http://repo.typesafe.com/typesafe/releases/")
      )
      result.plugins must beEqualTo(
        Seq(
          Artifact("com.typesafe.play", "sbt-plugin", "2.4.3", false),
          Artifact("org.scoverage", "sbt-scoverage", "1.0.1", true)
        )
      )
    }

  }

}

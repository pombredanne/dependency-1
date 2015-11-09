package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{Language, Library}
import org.specs2.mutable._

class ParseBuildSbtSpec extends Specification {

  "simple library with no dependencies" should {

    val contents = """
name := "lib-play"

lazy val root = project
  .in(file("."))
"""

    "parses dependencies" in {
      val result = ParseBuildSbt(contents)
      result.languages must beEqualTo(Nil)
      result.libraries must beEqualTo(Nil)
    }

  }

  "single project w/ dependencies" should {

    val contents = """
import play.PlayImport.PlayKeys._

organization := "io.flow"

scalaVersion in ThisBuild := "2.11.7"

crossScalaVersions := Seq("2.11.7")

lazy val root = project
  .in(file("."))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      ws,
      "io.flow" %% "lib-play-postgresql" % "0.0.1-SNAPSHOT",
      "org.postgresql" % "postgresql" % "9.4-1202-jdbc42"
    )
)
"""

    "parses dependencies" in {
      val result = ParseBuildSbt(contents)
      result.languages must beEqualTo(Seq(Language("scala", "2.11.7")))
      result.libraries must beEqualTo(
        Seq(
          Library("io.flow", "lib-play-postgresql", "0.0.1-SNAPSHOT"),
          Library("org.postgresql", "postgresql", "9.4-1202-jdbc42")
        )
      )
    }

  }
}

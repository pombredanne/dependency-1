package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{Language, Library}
import org.specs2.mutable._

class ParseBuildSbtSpec extends Specification {

  "simple library" should {

    val contents = """
import play.PlayImport.PlayKeys._

name := "lib-play"

organization := "io.flow"

scalaVersion in ThisBuild := "2.11.7"

crossScalaVersions := Seq("2.11.7")

version := "0.0.1-SNAPSHOT"

lazy val root = project
  .in(file("."))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      ws
    )
)
"""

    "parses dependencies" in {
      val result = ParseBuildSbt(contents)
      result.languages must beEqualTo(Seq(Language("scala", "2.11.7")))
      result.libraries must beEqualTo(Nil)
    }

  }
}

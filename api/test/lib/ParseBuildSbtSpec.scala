package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{LanguageForm, LibraryForm}
import org.specs2.mutable._

class ParseBuildSbtSpec extends Specification {

  "simple library with no dependencies" should {

    val contents = """
name := "lib-play"

lazy val root = project
  .in(file("."))
"""

    "parse dependencies" in {
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

    "parse dependencies" in {
      val result = ParseBuildSbt(contents)
      result.languages must beEqualTo(Seq(LanguageForm("scala", Some("2.11.7"))))
      result.libraries must beEqualTo(
        Seq(
          LibraryForm("io.flow", "lib-play-postgresql", Some("0.0.1-SNAPSHOT")),
          LibraryForm("org.postgresql", "postgresql", Some("9.4-1202-jdbc42"))
        )
      )
    }
 }

  "dependencies w/ comments" should {

    val contents = """
lazy val root = project
  .in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      ws,
      "io.flow" %% "lib-play-postgresql" % "0.0.1-SNAPSHOT" % Test, // Foo
      "org.postgresql" % "postgresql" % "9.4-1202-jdbc42" // Bar
    )
)
"""

    "parse dependencies" in {
      val result = ParseBuildSbt(contents)
      result.languages must beEqualTo(Nil)
      result.libraries must beEqualTo(
        Seq(
          LibraryForm("io.flow", "lib-play-postgresql", Some("0.0.1-SNAPSHOT")),
          LibraryForm("org.postgresql", "postgresql", Some("9.4-1202-jdbc42"))
        )
      )
    }
  }

  "multi project build w/ duplicates" should {

    val contents = """
lazy val api = project
  .in(file("api"))
  .settings(
    libraryDependencies ++= Seq(
      ws,
      "io.flow" %% "lib-play-postgresql" % "0.0.1-SNAPSHOT",
      "org.postgresql" % "postgresql" % "9.4-1202-jdbc42"
    )
)

lazy val www = project
  .in(file("api"))
  .settings(
    libraryDependencies ++= Seq(
      "io.flow" %% "lib-play-postgresql" % "0.0.2-SNAPSHOT",
      "org.postgresql" % "postgresql" % "9.4-1202-jdbc42"
    )
)
"""

    "parse dependencies" in {
      val result = ParseBuildSbt(contents)
      result.languages must beEqualTo(Nil)
      result.libraries must beEqualTo(
        Seq(
          LibraryForm("io.flow", "lib-play-postgresql", Some("0.0.1-SNAPSHOT")),
          LibraryForm("io.flow", "lib-play-postgresql", Some("0.0.2-SNAPSHOT")),
          LibraryForm("org.postgresql", "postgresql", Some("9.4-1202-jdbc42"))
        )
      )
    }
  }

  "library with variable version names" in {
    val contents = """
val avroVersion = "1.7.7"

lazy val avro = project
  .in(file("avro"))
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.avro"   % "avro"              % avroVersion,
      "org.apache.avro"   % "avro-compiler"     % avroVersion,
      "com.typesafe.play" %% "play-json" % "2.4.2",
      "org.scalatest"     %% "scalatest" % "2.2.0" % "test"
    )
  )
"""
    val result = ParseBuildSbt(contents)
    result.languages must beEqualTo(Nil)
    result.libraries must beEqualTo(
      Seq(
        LibraryForm("com.typesafe.play", "play-json", Some("2.4.2")),
        LibraryForm("org.apache.avro", "avro", Some("1.7.7")),
        LibraryForm("org.apache.avro", "avro-compiler", Some("1.7.7")),
        LibraryForm("org.scalatest", "scalatest", Some("2.2.0"))
      )
    )
  }

}

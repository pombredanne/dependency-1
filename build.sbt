import play.PlayImport.PlayKeys._
import scoverage.ScoverageSbtPlugin.ScoverageKeys._

name := "dependency"

organization := "io.flow"

scalaVersion in ThisBuild := "2.11.7"

// required because of issue between scoverage & sbt
parallelExecution in Test in ThisBuild := true

lazy val generated = project
  .in(file("generated"))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      ws
    )
  )

lazy val api = project
  .in(file("api"))
  .dependsOn(generated)
  .aggregate(generated)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    routesImport += "com.bryzek.dependency.v0.Bindables._",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      "io.flow" %% "lib-play" % "0.0.1-SNAPSHOT",
      "io.flow" %% "lib-play-postgresql" % "0.0.1-SNAPSHOT",
      "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.15",
      "org.postgresql" % "postgresql" % "9.4-1202-jdbc42",
      "org.scalatestplus" %% "play" % "1.4.0-M4" % "test"
    )
  )

lazy val www = project
  .in(file("www"))
  .dependsOn(generated)
  .aggregate(generated)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    routesImport += "com.bryzek.dependency.v0.Bindables._",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      "io.flow" %% "lib-play" % "0.0.1-SNAPSHOT",
      "org.webjars" %% "webjars-play" % "2.4.0-1",
      "org.webjars" % "bootstrap" % "3.3.5",
      "org.webjars" % "jquery" % "2.1.4"
    )
  )

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name <<= name("dependency-" + _),
  libraryDependencies ++= Seq(
    specs2 % Test,
    "org.scalatest" %% "scalatest" % "2.2.0" % Test
  ),
  scalacOptions += "-feature",
  coverageHighlighting := true,
  resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  credentials += Credentials(Path.userHome / ".ivy2" / ".artifactory")
)

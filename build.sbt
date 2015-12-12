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

// Allow api subproject to access the www router
lazy val router: Project = (project in file("router"))
  .dependsOn(generated)
  .aggregate(generated)
  .enablePlugins(PlayScala)
  .settings(
    routesImport += "com.bryzek.dependency.v0.Bindables._",
    aggregateReverseRoutes := Seq(api, www)
  )

lazy val lib = project
  .in(file("lib"))
  .dependsOn(generated, router)
  .aggregate(generated, router)
  .settings(commonSettings: _*)

lazy val api = project
  .in(file("api"))
  .dependsOn(generated, lib, router)
  .aggregate(generated, lib, router)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    routesImport += "com.bryzek.dependency.v0.Bindables._",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      "io.flow" %% "lib-play-postgresql" % "0.0.1",
      "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.16",
      "org.postgresql" % "postgresql" % "9.4-1206-jdbc42",
      "com.sendgrid"   %  "sendgrid-java" % "2.2.2",
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
      "org.webjars" %% "webjars-play" % "2.4.0-2",
      "org.webjars" % "bootstrap" % "3.3.6",
      "org.webjars.bower" % "bootstrap-social" % "4.10.1",
      "org.webjars" % "font-awesome" % "4.5.0",
      "org.webjars" % "jquery" % "2.1.4"
    )
  )

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name <<= name("dependency-" + _),
  libraryDependencies ++= Seq(
    "io.flow" %% "lib-play" % "0.0.3",
    specs2 % Test,
    "org.scalatest" %% "scalatest" % "2.2.5" % Test
  ),
  scalacOptions += "-feature",
  coverageHighlighting := true,
  resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  credentials += Credentials(Path.userHome / ".ivy2" / ".artifactory")
)

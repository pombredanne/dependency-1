 - when created a project, create a user_projects record for the user that is creating that project
 - add timeouts to api/app/lib/GithubDependencyProvider.scala
 - add support for sbt vesrion
 - use github sign in button from http://lipis.github.io/bootstrap-social/

parse resolvers from:

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
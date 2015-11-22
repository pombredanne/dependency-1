package com.bryzek.dependency.lib

import org.specs2.mutable._

class RemoteVersionsSpec extends Specification {

  "makeUrl" in {
    RemoteVersions.makeUrl("https://oss.sonatype.org/content/repositories/snapshots/", "com.github.tototoshi") must beEqualTo(
      "https://oss.sonatype.org/content/repositories/snapshots/com/github/tototoshi"
    )
  }

  "crossBuildVersion" in {
    RemoteVersions.crossBuildVersion("scala-csv_2.11/") must beEqualTo(Some("2.11"))
    RemoteVersions.crossBuildVersion("scala-csv_2.11") must beEqualTo(Some("2.11"))
    RemoteVersions.crossBuildVersion("scala-csv_2.10") must beEqualTo(Some("2.10"))
    RemoteVersions.crossBuildVersion("scala-csv_2.9.3") must beEqualTo(Some("2.9.3"))
    RemoteVersions.crossBuildVersion("scala-csv_2.9.3-dev") must beEqualTo(Some("2.9.3-dev"))

    RemoteVersions.crossBuildVersion("scala-csv") must beEqualTo(None)
    RemoteVersions.crossBuildVersion("scala-csv_foo") must beEqualTo(None)
  }

}

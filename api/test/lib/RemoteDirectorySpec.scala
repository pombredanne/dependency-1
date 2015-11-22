package com.bryzek.dependency.lib

import org.specs2.mutable._

class RemoteDirectorySpec extends Specification {

  "makeUrl" in {
    RemoteDirectory.makeUrl("https://oss.sonatype.org/content/repositories/snapshots/", "com.github.tototoshi") must beEqualTo(
      "https://oss.sonatype.org/content/repositories/snapshots/com/github/tototoshi"
    )
  }

  "crossBuildVersion" in {
    RemoteDirectory.crossBuildVersion("scala-csv_2.11/") must beEqualTo(Some("2.11"))
    RemoteDirectory.crossBuildVersion("scala-csv_2.11") must beEqualTo(Some("2.11"))
    RemoteDirectory.crossBuildVersion("scala-csv_2.10") must beEqualTo(Some("2.10"))
    RemoteDirectory.crossBuildVersion("scala-csv_2.9.3") must beEqualTo(Some("2.9.3"))
    RemoteDirectory.crossBuildVersion("scala-csv_2.9.3-dev") must beEqualTo(Some("2.9.3-dev"))

    RemoteDirectory.crossBuildVersion("scala-csv") must beEqualTo(None)
    RemoteDirectory.crossBuildVersion("scala-csv_foo") must beEqualTo(None)
  }

}

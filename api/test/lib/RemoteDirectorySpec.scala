package com.bryzek.dependency.lib

import org.specs2.mutable._

class RemoteDirectorySpec extends Specification {

  "makeUrl" in {
    RemoteDirectory.makeUrl("https://oss.sonatype.org/content/repositories/snapshots/", "com.github.tototoshi") must beEqualTo(
      "https://oss.sonatype.org/content/repositories/snapshots/com/github/tototoshi"
    )
  }

}

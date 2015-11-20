package com.bryzek.dependency.lib

import org.specs2.mutable._

class GitHubUtilSpec extends Specification {

  "parseUri" in {
    GitHubUtil.parseUri("http://github.com/mbryzek/apidoc") must beEqualTo(
      Right(
        GitHubUtil.Repository("mbryzek", "apidoc")
      )
    )
  }

  "parseUri for invalid URLs" in {
    GitHubUtil.parseUri("   ") must beEqualTo(
      Left(s"URI cannot be an empty string")
    )

    GitHubUtil.parseUri("http://github.com") must beEqualTo(
      Left("URI path cannot be empty for uri[http://github.com]")
    )

    GitHubUtil.parseUri("http://github.com/mbryzek") must beEqualTo(
      Left("Invalid uri path[http://github.com/mbryzek] missing project name")
    )

    GitHubUtil.parseUri("http://github.com/mbryzek/apidoc/other") must beEqualTo(
      Left("Invalid uri path[http://github.com/mbryzek/apidoc/other] - expected exactly two path components")
    )
  }

}

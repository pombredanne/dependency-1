package com.bryzek.dependency.lib

import org.specs2.mutable._

class GithubUtilSpec extends Specification {

  "parseUri" in {
    GithubUtil.parseUri("http://github.com/mbryzek/apidoc") must beEqualTo(
      Right(
        GithubUtil.Repository("mbryzek", "apidoc")
      )
    )
  }

  "parseUri for invalid URLs" in {
    GithubUtil.parseUri("   ") must beEqualTo(
      Left(s"URI cannot be an empty string")
    )

    GithubUtil.parseUri("http://github.com") must beEqualTo(
      Left("URI path cannot be empty for uri[http://github.com]")
    )

    GithubUtil.parseUri("http://github.com/mbryzek") must beEqualTo(
      Left("Invalid uri path[http://github.com/mbryzek] missing project name")
    )

    GithubUtil.parseUri("http://github.com/mbryzek/apidoc/other") must beEqualTo(
      Left("Invalid uri path[http://github.com/mbryzek/apidoc/other] - expected exactly two path components")
    )
  }

}

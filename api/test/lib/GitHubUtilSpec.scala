package com.bryzek.dependency.api.lib

import io.flow.user.v0.models.NameForm
import org.specs2.mutable._

class GithubUtilSpec extends Specification {

  "GithubHelper.parseName" in {
    GithubHelper.parseName("") must beEqualTo(NameForm())
    GithubHelper.parseName("  ") must beEqualTo(NameForm())
    GithubHelper.parseName("mike") must beEqualTo(NameForm(first = Some("mike")))
    GithubHelper.parseName("mike bryzek") must beEqualTo(NameForm(first = Some("mike"), last = Some("bryzek")))
    GithubHelper.parseName("   mike    maciej    bryzek  ") must beEqualTo(
      NameForm(first = Some("mike"), last = Some("maciej bryzek"))
    )
  }

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

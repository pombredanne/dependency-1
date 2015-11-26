package com.bryzek.dependency.lib

import org.scalatest.{FunSpec, Matchers}

class VersionTagSpec extends FunSpec with Matchers {

/*
  it("parses major from semver versions") {
    VersionTag("0.0.0").major should be(Some(0))
    VersionTag("0.0.0").major should be(Some(0))
    VersionTag("0.0.0-dev").major should be(Some(0))

    VersionTag("1.0.0").major should be(Some(1))
    VersionTag("1.0.0-dev").major should be(Some(1))
  }

  it("parses major from github versions") {
    VersionTag("v1").major should be(Some(1))
    VersionTag("v1.0.0").major should be(Some(1))
    VersionTag("v1.0.0-dev").major should be(Some(1))
  }

  it("returns none when no major number") {
    VersionTag("v").major should be(None)
    VersionTag("dev").major should be(None)
  }

  it("major ignores whitespace") {
    VersionTag(" 1.0").major should be(Some(1))
    VersionTag(" v2.0").major should be(Some(2))
  }

  it("nextMicro") {
    VersionTag("foo").nextMicro should be(None)
    VersionTag("foo-bar").nextMicro should be(None)
    VersionTag("foo-0.1.2").nextMicro should be(None)
    VersionTag("0.0.1").nextMicro should be(Some(VersionTag.Semver("0.0.2", 0, 0, 2)))
    VersionTag("1.2.3").nextMicro should be(Some(VersionTag.Semver("1.2.4", 1, 2, 4)))
    VersionTag("0.0.5-dev").nextMicro should be(None)
  }
 */

}

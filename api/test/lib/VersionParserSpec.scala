package com.bryzek.dependency.lib

import org.scalatest.{FunSpec, Matchers}

class VersionParserSpec extends FunSpec with Matchers {

  it("simple semver version numbers") {
    VersionParser.parse("1") should be(Version("1", Seq(Tag.Semver(1, 0, 0))))
    VersionParser.parse("1.0") should be(Version("1.0", Seq(Tag.Semver(1, 0, 0))))
    VersionParser.parse("1.0.0") should be(Version("1.0.0", Seq(Tag.Semver(1, 0, 0))))
    VersionParser.parse("1.2.3") should be(Version("1.2.3", Seq(Tag.Semver(1, 2, 3))))
    VersionParser.parse("1.2.3.4") should be(Version("1.2.3.4", Seq(Tag.Semver(1, 2, 3, Seq(4)))))
    VersionParser.parse("dev") should be(Version("dev", Seq(Tag.Text("dev"))))
    VersionParser.parse("1.0.0-dev") should be(Version("1.0.0-dev", Seq(Tag.Semver(1, 0, 0), Tag.Text("dev"))))
    VersionParser.parse("1.0.0_dev") should be(Version("1.0.0_dev", Seq(Tag.Semver(1, 0, 0), Tag.Text("dev"))))
    VersionParser.parse("1.0.0.dev") should be(Version("1.0.0.dev", Seq(Tag.Semver(1, 0, 0), Tag.Text("dev"))))
  }

  it("scala lang versions") {
    VersionParser.parse("2.9.1.final") should be(
      Version(
        "2.9.1.final",
        Seq(
          Tag.Semver(2, 9, 1),
          Tag.Text("final")
        )
      )
    )
  }
}

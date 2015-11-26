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

  it("isDate") {
    VersionParser.isDate(123) should be(false)
    VersionParser.isDate(20141018) should be(true)
    VersionParser.isDate(10141018) should be(false)
    VersionParser.isDate(19141018) should be(true)
  }

  it("date version numbers") {
    VersionParser.parse("123") should be(Version("123", Seq(Tag.Semver(123, 0, 0))))
    VersionParser.parse("20141018") should be(Version("20141018", Seq(Tag.Date(20141018, 0))))
    VersionParser.parse("20141018.1") should be(Version("20141018.1", Seq(Tag.Date(20141018, 1))))
    VersionParser.parse("r20141018.1") should be(Version("r20141018.1", Seq(Tag.Text("r"), Tag.Date(20141018, 1))))
    VersionParser.parse("10141018") should be(Version("10141018", Seq(Tag.Semver(10141018, 0, 0))))
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

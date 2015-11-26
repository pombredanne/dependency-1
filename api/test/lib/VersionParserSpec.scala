package com.bryzek.dependency.lib

import org.scalatest.{FunSpec, Matchers}

class VersionParserSpec extends FunSpec with Matchers {

  it("simple semver version numbers") {
    VersionParser.parse("1") should be(Version("1", Seq(Tag.Semver(1, 0, 0))))
    VersionParser.parse("1.0") should be(Version("1.0", Seq(Tag.Semver(1, 0, 0))))
    VersionParser.parse("1.0.0") should be(Version("1.0.0", Seq(Tag.Semver(1, 0, 0))))
    VersionParser.parse("1.2.3") should be(Version("1.2.3", Seq(Tag.Semver(1, 2, 3))))
    VersionParser.parse("r1.2.3") should be(Version("r1.2.3", Seq(Tag.Semver(1, 2, 3))))
    VersionParser.parse("V1.2.3") should be(Version("V1.2.3", Seq(Tag.Semver(1, 2, 3))))
    VersionParser.parse("experimental1.2.3") should be(Version("experimental1.2.3", Seq(Tag.Text("experimental"), Tag.Semver(1, 2, 3))))
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
    VersionParser.parse("r20141018.1") should be(Version("r20141018.1", Seq(Tag.Date(20141018, 1))))
    VersionParser.parse("10141018") should be(Version("10141018", Seq(Tag.Semver(10141018, 0, 0))))
  }

  it("postgresql version") {
    VersionParser.parse("9.4-1201-jdbc41") should be(
      Version(
        "9.4-1201-jdbc41",
        Seq(
          Tag.Semver(9, 4, 0),
          Tag.Semver(1201, 0, 0),
          Tag.Text("jdbc"),
          Tag.Semver(41, 0, 0)
        )
      )
    )
  }

  it("separated text from numbers") {
    VersionParser.parse("1.4.0-M4") should be(
      Version(
        "1.4.0-M4",
        Seq(
          Tag.Semver(1, 4, 0),
          Tag.Text("M"),
          Tag.Semver(4, 0, 0)
        )
      )
    )
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

  it("sortKey") {
    VersionParser.parse("TEST").sortKey should be("20.test")
    VersionParser.parse("r20141211.1").sortKey should be("40.20141211.10001")
    VersionParser.parse("1.2.3").sortKey should be("80.10001.10002.10003")
    VersionParser.parse("r1.2.3").sortKey should be("80.10001.10002.10003")
    VersionParser.parse("1.2.3.4").sortKey should be("80.10001.10002.10003.10004")

    VersionParser.parse("1.0.0").sortKey should be("80.10001.10000.10000")
    VersionParser.parse("1.0.0-g-1").sortKey should be("60.10001.10000.10000,20.g,60.10001.10000.10000")
  }

  it("sorts 1 element version") {
    assertSorted(Seq("0", "1", "5"), "0 1 5")
    assertSorted(Seq("5", "0", "1"), "0 1 5")
    assertSorted(Seq("2", "1", "0"), "0 1 2")
  }

  it("sorts 2 element version") {
    assertSorted(Seq("0.0", "0.1", "2.1"), "0.0 0.1 2.1")
    assertSorted(Seq("0.0", "0.1", "2.1"), "0.0 0.1 2.1")
    assertSorted(Seq("1.0", "0.0", "1.1", "1.2", "0.10"), "0.0 0.10 1.0 1.1 1.2")
  }

  it("sorts 3 element version") {
    assertSorted(Seq("0.0.0", "0.0.1", "0.1.0", "5.1.0"), "0.0.0 0.0.1 0.1.0 5.1.0")
    assertSorted(Seq("10.10.10", "10.0.1", "1.1.50", "15.2.2", "1.0.10"), "1.0.10 1.1.50 10.0.1 10.10.10 15.2.2")
  }

  it("sorts developer tags before release tags (latest release tag should be last)") {
    assertSorted(Seq("1.0.0", "1.0.0-g-1"), "1.0.0-g-1 1.0.0")
    assertSorted(Seq("0.6.0-3-g3b52fba", "0.7.6"), "0.6.0-3-g3b52fba 0.7.6")

    assertSorted(Seq("0.28.1", "0.28.1-dev"), "0.28.1-dev 0.28.1")
    assertSorted(Seq("0.28.1-dev", "0.28.1"), "0.28.1-dev 0.28.1")
  }

  it("sorts string tags as strings") {
    assertSorted(Seq("r20140201.1", "r20140201.2"), "r20140201.1 r20140201.2")
  }

  it("sorts strings mixed with semver tags") {
    assertSorted(Seq("0.8.6", "0.8.8", "development"), "development 0.8.6 0.8.8")
  }

  it("sorts versions w/ varying lengths") {
    assertSorted(Seq("1", "0.1"), "0.1 1")
    assertSorted(Seq("1", "0.1", "0.0.1"), "0.0.1 0.1 1")
    assertSorted(Seq("1.2", "1.2.1"), "1.2 1.2.1")
    assertSorted(Seq("1.2", "1.2.1", "2"), "1.2 1.2.1 2")
  }

  it("Sorts semvers with more than 3 components") {
    assertSorted(Seq("1.0.9.5", "1.0.9.8", "1.0.10.1", "1.0.10.2"), "1.0.9.5 1.0.9.8 1.0.10.1 1.0.10.2")
  }

  it("numeric tags are considered newer than string tags") {
    assertSorted(Seq("1.0.0", "r20140201.1"), "r20140201.1 1.0.0")
  }

  it("scalatestplus version numbers") {
    assertSorted(Seq("1.4.0-M4", "1.4.0-M3"), "1.4.0-M3 1.4.0-M4")
  }

  def assertSorted(versions: Seq[String], target: String) {
    versions.map( VersionParser.parse(_) ).sorted.map(_.value).mkString(" ") should be(target)
  }
}

package com.bryzek.dependency.lib

import org.scalatest.{FunSpec, Matchers}

// From https://github.com/mbryzek/apidoc
class VersionTag2Spec extends FunSpec with Matchers {

  def assertSorted(versions: Seq[String], target: String) {
    versions.map( VersionTag2(_) ).foreach { v =>
      println(s" - $v: ${v.sortKey}")
    }
    versions.map( VersionTag2(_) ).sorted.map(_.version).mkString(" ") should be(target)
  }

  it("isDate") {
    VersionTag2.isDate(123) should be(false)
    VersionTag2.isDate(20141018) should be(true)
    VersionTag2.isDate(10141018) should be(false)
    VersionTag2.isDate(19141018) should be(true)
  }

  it("fromString") {
    VersionTag2("1") should be(VersionTag2.Semver("1", 1, 0, 0))
    VersionTag2("1.0") should be(VersionTag2.Semver("1.0", 1, 0, 0))
    VersionTag2("1.0.0") should be(VersionTag2.Semver("1.0.0", 1, 0, 0))
    VersionTag2("1.2.3.4") should be(VersionTag2.Semver("1.2.3.4", 1, 2, 3))
    VersionTag2("dev") should be(VersionTag2.Unknown("dev"))

    VersionTag2("1.0.0-dev") should be(
      VersionTag2.Multi(
        "1.0.0-dev",
        Seq(
          VersionTag2.Semver("1.0.0", 1, 0, 0),
          VersionTag2.Unknown("dev")
        )
      )
    )
  }

  it("sorts developer tags before release tags (latest release tag should be last)") {
    assertSorted(Seq("1.0.0", "1.0.0-g-1"), "1.0.0-g-1 1.0.0")
    assertSorted(Seq("0.6.0-3-g3b52fba", "0.7.6"), "0.6.0-3-g3b52fba 0.7.6")

    assertSorted(Seq("0.28.1", "0.28.1-dev"), "0.28.1-dev 0.28.1")
    assertSorted(Seq("0.28.1-dev", "0.28.1"), "0.28.1-dev 0.28.1")
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

  it("sorts string tags as strings") {
    assertSorted(Seq("r20140201.1", "r20140201.2"), "r20140201.1 r20140201.2")
  }

  it("sorts strings mixed with semver tags") {
    assertSorted(Seq("0.8.6", "0.8.8", "development"), "development 0.8.6 0.8.8")
  }

  it("parses major from semver versions") {
    VersionTag2("0.0.0").major should be(Some(0))
    VersionTag2("0.0.0").major should be(Some(0))
    VersionTag2("0.0.0-dev").major should be(Some(0))

    VersionTag2("1.0.0").major should be(Some(1))
    VersionTag2("1.0.0-dev").major should be(Some(1))
  }

  it("parses major from github versions") {
    VersionTag2("v1").major should be(Some(1))
    VersionTag2("v1.0.0").major should be(Some(1))
    VersionTag2("v1.0.0-dev").major should be(Some(1))
  }

  it("returns none when no major number") {
    VersionTag2("v").major should be(None)
    VersionTag2("dev").major should be(None)
  }

  it("major ignores whitespace") {
    VersionTag2(" 1.0").major should be(Some(1))
    VersionTag2(" v2.0").major should be(Some(2))
  }

  it("sorts versions w/ varying lengths") {
    assertSorted(Seq("1", "0.1"), "0.1 1")
    assertSorted(Seq("1", "0.1", "0.0.1"), "0.0.1 0.1 1")
    assertSorted(Seq("1.2", "1.2.1"), "1.2 1.2.1")
    assertSorted(Seq("1.2", "1.2.1", "2"), "1.2 1.2.1 2")

  }

  it("numeric tags are considered newer than string tags") {
    assertSorted(Seq("1.0.0", "r20140201.1"), "r20140201.1 1.0.0")
  }

  it("scalatestplus version numbers") {
    assertSorted(Seq("1.4.0-M4", "1.4.0-M3"), "1.4.0-M3 1.4.0-M4")
  }

  it("Sorts semvers with more than 3 components") {
    // we don't fully support this use case... defaults to a string
    // sort after position 3

    assertSorted(Seq("1.0.9.5", "1.0.9.8", "1.0.10.1", "1.0.10.2"), "1.0.9.5 1.0.9.8 1.0.10.1 1.0.10.2")
  }

  it("nextMicro") {
    VersionTag2("foo").nextMicro should be(None)
    VersionTag2("foo-bar").nextMicro should be(None)
    VersionTag2("foo-0.1.2").nextMicro should be(None)
    VersionTag2("0.0.1").nextMicro should be(Some(VersionTag2.Semver("0.0.2", 0, 0, 2)))
    VersionTag2("1.2.3").nextMicro should be(Some(VersionTag2.Semver("1.2.4", 1, 2, 4)))

    VersionTag2("0.0.5-dev").nextMicro should be(Some(VersionTag2.Multi(
      "0.0.6-dev",
      Seq(
        VersionTag2.Semver("0.0.6", 0, 0, 6),
        VersionTag2.Unknown("dev")
      )
    )))
  }

  it("qualifier") {
    VersionTag2("foo").qualifier should be(None)
    VersionTag2("0.0.1").qualifier should be(None)
    VersionTag2("0.0.5-dev").qualifier should be(Some("dev"))
  }

}

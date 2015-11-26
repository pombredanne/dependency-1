package com.bryzek.dependency.lib

import org.scalatest.{FunSpec, Matchers}

class VersionTagSpec extends FunSpec with Matchers {

  def assertSorted(versions: Seq[String], target: String) {
    versions.map( VersionTag(_) ).sorted.map(_.version).mkString(" ") should be(target)
  }
/*
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
    VersionTag("foo").nextMicro should be(None)
    VersionTag("foo-bar").nextMicro should be(None)
    VersionTag("foo-0.1.2").nextMicro should be(None)
    VersionTag("0.0.1").nextMicro should be(Some(VersionTag.Semver("0.0.2", 0, 0, 2)))
    VersionTag("1.2.3").nextMicro should be(Some(VersionTag.Semver("1.2.4", 1, 2, 4)))
    VersionTag("0.0.5-dev").nextMicro should be(None)
  }

  it("postgresql version") {
    VersionTag("9.4-1201-jdbc41") should be(
      VersionTag.Multi(
        "9.4-1201-jdbc41",
        Seq(
          VersionTag.Semver("9.4", 9, 4, 0),
          VersionTag.Semver("1201", 1201, 0, 0),
          VersionTag.Text("jdbc41")
        )
      )
    )
  }

  it("separated text from numbers") {
    VersionTag("1.4.0-M4") should be(
      VersionTag.Multi(
        "1.4.0-M4",
        Seq(
          VersionTag.Semver("1.4.0", 1, 4, 0),
          VersionTag.Text("M"),
          VersionTag.Semver("4", 4, 0, 0)
        )
      )
    )
  }
 */

  it("scala lang versions") {
    VersionTag("2.9.1.final") should be(VersionTag.Multi(
      "2.9.1.final",
      Seq(
        VersionTag.Semver("2.9.1", 2, 9, 1),
        VersionTag.Text("final")
      )
    ))
  }

}

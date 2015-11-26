package com.bryzek.dependency.lib

import org.scalatest.{FunSpec, Matchers}

class VersionTagSpec extends FunSpec with Matchers {

  def assertSorted(versions: Seq[String], target: String) {
<<<<<<< 751557e36cd1584890a1874d0f57f2a19be5fd2c
<<<<<<< a6c67135e2a8eb0c2d372c491fe8ffd006bddae9
<<<<<<< ac10373e505f3e49259b7a31c5e260e2cace0ed3
<<<<<<< 7dec3a0762eb1988b8b61393530618e8b956c3c2
<<<<<<< Updated upstream
    val versionObjects = versions.map( VersionTag(_) )
=======
    val versionObjects = versions.map( VersionTag2(_) )
    versionObjects.foreach { v =>
      println(s" - $v: ${v.sortKey}")
    }
>>>>>>> Refactor version parsing
    versionObjects.sorted.map(_.version).mkString(" ") should be(target)
=======
    versions.map( VersionTag2(_) ).foreach { v =>
=======
    versions.map( VersionTag(_) ).foreach { v =>
>>>>>>> Replace VersionTag implementation
      println(s" - $v: ${v.sortKey}")
    }
=======
>>>>>>> Remove debugging
    versions.map( VersionTag(_) ).sorted.map(_.version).mkString(" ") should be(target)
  }

<<<<<<< c205d2d7faf75389cfbc9a1db6604b82ec09e13a
<<<<<<< 087c2ed15f53566e2e491ca959a845d7d74e1b15
/*
=======
>>>>>>> Add support for date based tags
  it("isDate") {
    VersionTag.isDate(123) should be(false)
    VersionTag.isDate(20141018) should be(true)
    VersionTag.isDate(10141018) should be(false)
    VersionTag.isDate(19141018) should be(true)
  }
<<<<<<< c205d2d7faf75389cfbc9a1db6604b82ec09e13a

  it("fromString") {
    VersionTag2("1") should be(VersionTag2.Semver("1", 1, 0, 0))
    VersionTag2("1.0") should be(VersionTag2.Semver("1.0", 1, 0, 0))
    VersionTag2("1.0.0") should be(VersionTag2.Semver("1.0.0", 1, 0, 0))
    VersionTag2("1.0.0-dev") should be(VersionTag2.QualifiedSemver("1.0.0-dev", 1, 0, 0, "dev"))
    VersionTag2("dev") should be(VersionTag2.Unknown("dev"))
>>>>>>> Stashed changes
=======
    versions.map( VersionTag2(_) ).foreach { v =>
      println(s" - $v: ${v.sortKey}")
    }
    versions.map( VersionTag2(_) ).sorted.map(_.version).mkString(" ") should be(target)
>>>>>>> Implement next micro
  }
=======
  /*
>>>>>>> Add Date version tag
=======
>>>>>>> Add support for date based tags

  it("fromString") {
    VersionTag("1") should be(VersionTag.Semver("1", 1, 0, 0))
    VersionTag("1.0") should be(VersionTag.Semver("1.0", 1, 0, 0))
    VersionTag("1.0.0") should be(VersionTag.Semver("1.0.0", 1, 0, 0))
    VersionTag("1.2.3.4") should be(VersionTag.Semver("1.2.3.4", 1, 2, 3))
    VersionTag("dev") should be(VersionTag.Unknown("dev"))

    VersionTag("1.0.0-dev") should be(
      VersionTag.Multi(
        "1.0.0-dev",
        Seq(
          VersionTag.Semver("1.0.0", 1, 0, 0),
          VersionTag.Unknown("dev")
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

<<<<<<< 47f106d8025363bbd8426db41d2bd38d6633abd1
<<<<<<< Updated upstream
  it("nextMicro") {
    VersionTag2("foo").nextMicro should be(None)
    VersionTag2("0.0.1").nextMicro should be(Some(VersionTag2.Semver("0.0.2", 0, 0, 2)))
    VersionTag2("1.2.3").nextMicro should be(Some(VersionTag2.Semver("1.2.4", 1, 2, 4)))
    VersionTag2("0.0.5-dev").nextMicro should be(Some(VersionTag2.QualifiedSemver("0.0.6-dev", 0, 0, 6, "dev")))
  }

  it("qualifier") {
    VersionTag2("foo").qualifier should be(None)
    VersionTag2("0.0.1").qualifier should be(None)
    VersionTag2("0.0.5-dev").qualifier should be(Some("dev"))
  }

=======
>>>>>>> Stashed changes
=======
>>>>>>> Complete version tag implementation
  it("sorts versions w/ varying lengths") {
    assertSorted(Seq("1", "0.1"), "0.1 1")
    assertSorted(Seq("1", "0.1", "0.0.1"), "0.0.1 0.1 1")
    assertSorted(Seq("1.2", "1.2.1"), "1.2 1.2.1")
    assertSorted(Seq("1.2", "1.2.1", "2"), "1.2 1.2.1 2")

  }
<<<<<<< c205d2d7faf75389cfbc9a1db6604b82ec09e13a
<<<<<<< 087c2ed15f53566e2e491ca959a845d7d74e1b15
<<<<<<< d85e41e24a7810d0c389fe0888b91a12a6250a78
<<<<<<< 7dec3a0762eb1988b8b61393530618e8b956c3c2

<<<<<<< Updated upstream
=======
  it("numeric tags are considered newer than string tags") {
    assertSorted(Seq("1.0.0", "r20140201.1"), "r20140201.1 1.0.0")
  }

  it("nextMicro") {
    VersionTag2("foo").nextMicro should be(None)
    VersionTag2("0.0.1").nextMicro should be(Some(VersionTag2.Semver("0.0.2", 0, 0, 2)))
    VersionTag2("1.2.3").nextMicro should be(Some(VersionTag2.Semver("1.2.4", 1, 2, 4)))
    VersionTag2("0.0.5-dev").nextMicro should be(Some(VersionTag2.QualifiedSemver("0.0.6-dev", 0, 0, 6, "dev")))
  }
*/
  it("qualifier") {
    VersionTag2("foo").qualifier should be(None)
    VersionTag2("0.0.1").qualifier should be(None)
    VersionTag2("0.0.5-dev").qualifier should be(Some("dev"))
  }

>>>>>>> Stashed changes
=======
   */
>>>>>>> Refactor version parsing
=======
=======
  */

=======
>>>>>>> Add support for date based tags

  it("numeric tags are considered newer than string tags") {
    assertSorted(Seq("1.0.0", "r20140201.1"), "r20140201.1 1.0.0")
  }
>>>>>>> Add Date version tag

<<<<<<< 47f106d8025363bbd8426db41d2bd38d6633abd1
>>>>>>> Implement parsing of addl version numbers
=======
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

<<<<<<< ff47b6162c260c64cb03adb3da4177f5d40c04a7
>>>>>>> Complete version tag implementation
=======
  it("postgresql version") {
    VersionTag("9.4-1201-jdbc41") should be(
      VersionTag.Multi(
        "9.4-1201-jdbc41",
        Seq(
          VersionTag.Semver("9.4", 9, 4, 0),
          VersionTag.Semver("1201", 1201, 0, 0),
          VersionTag.Unknown("jdbc41")
        )
      )
    )
  }

<<<<<<< 11fb6778b8bc5b9043f3568ed4be7d3688f0e867
>>>>>>> Add test to parse postgresql version number
=======
  it("scala lang versions") {
    VersionTag("2.9.1.final") should be(VersionTag.Multi(
      "2.9.1.final",
      Seq(
        VersionTag.Semver("2.9.1", 2, 9, 1),
        VersionTag.Unknown("final")
      )
    ))
  }

>>>>>>> Add support for scala versions - e.g. 2.9.1.final
}

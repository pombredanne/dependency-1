package com.bryzek.dependency.lib

import org.specs2.mutable._

class RecommendationsSpec extends Specification {

  "No recommendation if others is empty" in {
    Recommendations.version("1.0.0", Nil) must be(None)
  }

  "No recommendation if others is self" in {
    Recommendations.version("1.0.0", Seq("1.0.0")) must be(None)
  }

  "No recommendation if others are lower than self" in {
    Recommendations.version("1.0.0", Seq("0.1.0", "0.1.1")) must be(None)
  }

  "No recommendation if greater versions are beta versions" in {
    Recommendations.version("1.0.0", Seq("1.0.1-rc1")) must be(None)
  }

  "postgresql example" in {
    Recommendations.version(
      "9.4-1201-jdbc41",
      Seq("9.4-1205-jdbc4", "9.4-1205-jdbc41", "9.4-1205-jdbc42")
    ) must beEqualTo(Some("9.4-1205-jdbc42"))
  }

  "scalatest example" in {
    Recommendations.version(
      "1.4.0-M3",
      Seq("1.4.0-M3", "1.4.0-M4", "1.4.0-SNAP1")
    ) must beEqualTo(Some("1.4.0-M4"))
  }

}

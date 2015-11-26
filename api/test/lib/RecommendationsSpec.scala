package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.VersionForm
import org.specs2.mutable._

class RecommendationsSpec extends Specification {

  def simpleRecs(value: String, others: Seq[String]) = {
    Recommendations.version(
      VersionForm(value),
      others.map(VersionForm(_))
    )
  }

  "No recommendation if others is empty" in {
    simpleRecs("1.0.0", Nil) must be(None)
  }

  "No recommendation if others is self" in {
    simpleRecs("1.0.0", Seq("1.0.0")) must be(None)
  }

  "No recommendation if others are lower than self" in {
    simpleRecs("1.0.0", Seq("0.1.0", "0.1.1")) must be(None)
  }

  "No recommendation if greater versions are beta versions" in {
    simpleRecs("1.0.0", Seq("1.0.1-rc1")) must be(None)
  }

  "postgresql example" in {
    simpleRecs(
      "9.4-1201-jdbc41",
      Seq("9.4-1205-jdbc4", "9.4-1205-jdbc41", "9.4-1205-jdbc42")
    ) must beEqualTo(Some("9.4-1205-jdbc42"))
  }

  "scalatest example" in {
    simpleRecs(
      "1.4.0-M3",
      Seq("1.4.0-M3", "1.4.0-M4", "1.4.0-SNAP1")
    ) must beEqualTo(Some("1.4.0-M4"))
  }

//1.3.3	0.13
//1.3.2	0.13
//1.0.1	2.11.7

}

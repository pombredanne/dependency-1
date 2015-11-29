package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.LanguageForm
import org.specs2.mutable._

class DependencyHelperSpec extends Specification {

  "crossBuildVersion for scala" in {
    Seq("2.10", "2.10.1", "2.10.1-RC1", "2.10.0-M3").map { tag =>
      DependencyHelper.crossBuildVersion(
        LanguageForm("scala", tag)
      ) must beEqualTo(Version("2.10", Seq(Tag.Semver(2, 10, 0))))
    }

    Seq("2.11", "2.11.1", "2.11.1-RC1", "2.11.0-M3").map { tag =>
      DependencyHelper.crossBuildVersion(
        LanguageForm("scala", tag)
      ) must beEqualTo(Version("2.11", Seq(Tag.Semver(2, 11, 0))))
    }
  }

  "crossBuildVersion for other languages uses whole version" in {
    Seq("2.10", "2.10.1", "2.10.1-RC1", "2.10.0-M3").map { tag =>
      DependencyHelper.crossBuildVersion(
        LanguageForm("other", tag)
      ).value must beEqualTo(tag)
    }
  }

}
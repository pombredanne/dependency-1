package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.LanguageForm
import org.specs2.mutable._

class SimpleScalaParserSpec extends Specification {

  "definesVariable" in {
    SimpleScalaParserUtil.definesVariable("var foo = 3") should beTrue
    SimpleScalaParserUtil.definesVariable("val foo = 3") should beTrue
    SimpleScalaParserUtil.definesVariable("lazy var foo = 3") should beTrue
    SimpleScalaParserUtil.definesVariable("lazy val foo = 3") should beTrue
    SimpleScalaParserUtil.definesVariable("foo := 3") should beFalse
  }

  "definesVariable tolerates spaces" in {
    SimpleScalaParserUtil.definesVariable("   val     foo = 3") should beTrue
    SimpleScalaParserUtil.definesVariable("   lazy    val     foo = 3") should beTrue
  }

}

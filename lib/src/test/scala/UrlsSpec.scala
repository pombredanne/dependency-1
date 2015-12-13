package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{ItemSummaryUndefinedType, RecommendationType}
import org.scalatest.{FunSpec, Matchers}

class UrlsSpec extends FunSpec with Matchers with Factories {

  it("recommendation") {
    val binary = makeRecommendation(`type` = RecommendationType.Binary)
    Urls.recommendation(binary) should be(s"/binaries/${binary.`object`.guid}")

    val library = makeRecommendation(`type` = RecommendationType.Library)
    Urls.recommendation(library) should be(s"/libraries/${library.`object`.guid}")

    val other = makeRecommendation(`type` = RecommendationType.UNDEFINED("other"))
    Urls.recommendation(other) should be("#")
  }

  it("itemSummary") {
    val binary = makeBinarySummary()
    Urls.itemSummary(binary) should be(s"/binaries/${binary.guid}")

    val library = makeLibrarySummary()
    Urls.itemSummary(library) should be(s"/libraries/${library.guid}")

    val project = makeProjectSummary()
    Urls.itemSummary(project) should be(s"/projects/${project.guid}")

    Urls.itemSummary(ItemSummaryUndefinedType("other")) should be("#")
  }

}

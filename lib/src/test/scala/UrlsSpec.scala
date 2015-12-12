package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{ItemSummaryUndefinedType, RecommendationType}
import org.scalatest.{FunSpec, Matchers}

class UrlsSpec extends FunSpec with Matchers with Factories {

  it("recommendationUrl") {
    val binary = makeRecommendation(`type` = RecommendationType.Binary)
    Urls.recommendationUrl(binary) should be(s"/binaries/${binary.`object`.guid}")

    val library = makeRecommendation(`type` = RecommendationType.Library)
    Urls.recommendationUrl(library) should be(s"/libraries/${library.`object`.guid}")

    val other = makeRecommendation(`type` = RecommendationType.UNDEFINED("other"))
    Urls.recommendationUrl(other) should be("#")
  }

  it("itemSummaryUrl") {
    val binary = makeBinarySummary()
    Urls.itemSummaryUrl(binary) should be(s"/binaries/${binary.guid}")

    val library = makeLibrarySummary()
    Urls.itemSummaryUrl(library) should be(s"/libraries/${library.guid}")

    val project = makeProjectSummary()
    Urls.itemSummaryUrl(project) should be(s"/projects/${project.guid}")

    Urls.itemSummaryUrl(ItemSummaryUndefinedType("other")) should be("#")
  }

}

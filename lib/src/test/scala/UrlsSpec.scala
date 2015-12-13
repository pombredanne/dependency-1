package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{ItemSummaryUndefinedType, RecommendationType}
import org.scalatest.{FunSpec, Matchers}

class UrlsSpec extends FunSpec with Matchers with Factories {

  private[this] lazy val urls = Urls()

  it("recommendation") {
    val binary = makeRecommendation(`type` = RecommendationType.Binary)
    urls.recommendation(binary) should be(s"/binaries/${binary.`object`.guid}")

    val library = makeRecommendation(`type` = RecommendationType.Library)
    urls.recommendation(library) should be(s"/libraries/${library.`object`.guid}")

    val other = makeRecommendation(`type` = RecommendationType.UNDEFINED("other"))
    urls.recommendation(other) should be("#")
  }

  it("itemSummary") {
    val binary = makeBinarySummary()
    urls.itemSummary(binary) should be(s"/binaries/${binary.guid}")

    val library = makeLibrarySummary()
    urls.itemSummary(library) should be(s"/libraries/${library.guid}")

    val project = makeProjectSummary()
    urls.itemSummary(project) should be(s"/projects/${project.guid}")

    urls.itemSummary(ItemSummaryUndefinedType("other")) should be("#")
  }

}

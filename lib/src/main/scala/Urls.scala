package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{BinarySummary, ItemSummary, ItemSummaryUndefinedType, LibrarySummary, ProjectSummary}
import com.bryzek.dependency.v0.models.{Recommendation, RecommendationType}

object Urls {

  def recommendationUrl(recommendation: Recommendation): String = {
    recommendation.`type` match {
      case RecommendationType.Library => controllers.routes.LibrariesController.show(recommendation.`object`.guid).url
      case RecommendationType.Binary => controllers.routes.BinariesController.show(recommendation.`object`.guid).url
      case RecommendationType.UNDEFINED(_) => "#"
    }
  }

  def itemSummaryUrl(summary: ItemSummary): String = {
    summary match {
      case BinarySummary(guid, _) => controllers.routes.BinariesController.show(guid).url
      case LibrarySummary(guid, _, _) => controllers.routes.LibrariesController.show(guid).url
      case ProjectSummary(guid, _) => controllers.routes.ProjectsController.show(guid).url
      case ItemSummaryUndefinedType(_) => "#"
    }
  }

}

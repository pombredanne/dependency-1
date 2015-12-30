package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{BinarySummary, BinaryType, ItemSummary, ItemSummaryUndefinedType, LibrarySummary, OrganizationSummary, ProjectSummary}
import com.bryzek.dependency.v0.models.{ProjectDetail, Recommendation, RecommendationType}
import io.flow.common.v0.models.Reference
import io.flow.play.clients.MockUserClient

trait Factories {

  def makeName(): String = {
    s"Z Test ${UUID.randomUUID}"
  }

  def makeKey(): String = {
    "z-test-${UUID.randomUUID.toString.toLowerCase}"
  }

  def makeRecommendation(
    `type`: RecommendationType = RecommendationType.Library
  ) = Recommendation(
    id = UUID.randomUUID,
    project = ProjectDetail(
      id = UUID.randomUUID,
      organization = makeOrganizationSummary(),
      name = makeName()
    ),
    `type` = `type`,
    `object` = Reference(UUID.randomUUID),
      name = "io.flow.lib-play",
    from = "0.0.1",
    to = "0.0.1",
    audit = MockUserClient.makeAudit()
  )

  def makeBinarySummary(
    id: String = UUID.randomUUID,
    `type`: BinaryType = BinaryType.Scala
  ) = BinarySummary(
    id = id,
    organization = makeOrganizationSummary(),
    name = `type`
  )

  def makeLibrarySummary(
    id: String = UUID.randomUUID,
    groupId: String = "io.flow",
    artifactId: String = "lib-play"
  ) = LibrarySummary(
    id = id,
    organization = makeOrganizationSummary(),
    groupId = groupId,
    artifactId = artifactId
  )

  def makeProjectSummary(
    id: String = UUID.randomUUID,
    name: String = makeName()
  ) = ProjectSummary(
    id = id,
    organization = makeOrganizationSummary(),
    name = name
  )

  def makeOrganizationSummary(
    id: String = UUID.randomUUID,
    key: String = makeKey()
  ) = OrganizationSummary(
    id = id,
    key = key
  )

}

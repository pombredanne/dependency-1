package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{BinarySummary, BinaryType, ItemSummary, ItemSummaryUndefinedType, LibrarySummary, OrganizationSummary, ProjectSummary}
import com.bryzek.dependency.v0.models.{ProjectDetail, Recommendation, RecommendationType}
import io.flow.common.v0.models.Reference
import io.flow.play.clients.MockUserClient
import java.util.UUID

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
    guid = UUID.randomUUID,
    project = ProjectDetail(
      guid = UUID.randomUUID,
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
    guid: UUID = UUID.randomUUID,
    `type`: BinaryType = BinaryType.Scala
  ) = BinarySummary(
    guid = guid,
    organization = makeOrganizationSummary(),
    name = `type`
  )

  def makeLibrarySummary(
    guid: UUID = UUID.randomUUID,
    groupId: String = "io.flow",
    artifactId: String = "lib-play"
  ) = LibrarySummary(
    guid = guid,
    organization = makeOrganizationSummary(),
    groupId = groupId,
    artifactId = artifactId
  )

  def makeProjectSummary(
    guid: UUID = UUID.randomUUID,
    name: String = makeName()
  ) = ProjectSummary(
    guid = guid,
    organization = makeOrganizationSummary(),
    name = name
  )

  def makeOrganizationSummary(
    guid: UUID = UUID.randomUUID,
    key: String = makeKey()
  ) = OrganizationSummary(
    guid = guid,
    key = key
  )

}

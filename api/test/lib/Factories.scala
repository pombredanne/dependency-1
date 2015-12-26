package com.bryzek.dependency.api.lib

import com.bryzek.dependency.v0.models.{ProjectSummary, OrganizationSummary, ResolverSummary, Visibility}
import java.util.UUID

trait Factories {

  def makeName(): String = {
    s"Z Test ${UUID.randomUUID}"
  }

  def makeKey(): String = {
    s"z-test-${UUID.randomUUID.toString.toLowerCase}"
  }

  def makeOrganizationSummary(
    guid: UUID = UUID.randomUUID,
    key: String = makeKey()
  ) = OrganizationSummary(
    guid = guid,
    key = key
  )

  def makeProjectSummary(
    guid: UUID = UUID.randomUUID,
    org: OrganizationSummary = makeOrganizationSummary(),
    name: String = makeName
  ) = ProjectSummary(
    guid = guid,
    organization = org,
    name = name
  )

  def makeResolverSummary(
    guid: UUID = UUID.randomUUID,
    org: OrganizationSummary = makeOrganizationSummary(),
    name: String = makeName
  ) = ResolverSummary(
    guid = guid,
    organization = Some(org),
    visibility = Visibility.Private,
    uri = "http://" + makeKey() + ".test.flow.io"
  )

}

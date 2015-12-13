package com.bryzek.dependency.api.lib

import com.bryzek.dependency.v0.models.{LibraryForm, OrganizationSummary, Resolver, VersionForm}

case class Artifact(
  org: OrganizationSummary,
  groupId: String,
  artifactId: String,
  version: String,
  isCrossBuilt: Boolean
) {

  def toLibraryForm(
    crossBuildVersion: Option[Version]
  ): LibraryForm = {
    LibraryForm(
      organizationGuid = org.guid,
      groupId = groupId,
      artifactId = artifactId,
      version = Some(
        VersionForm(
          version = version,
          crossBuildVersion = isCrossBuilt match {
            case true => crossBuildVersion.map(_.value)
            case false => None
          }
        )
      )
    )
  }

}

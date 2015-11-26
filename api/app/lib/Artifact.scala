package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{LibraryForm, VersionForm}

case class Artifact(
  groupId: String,
  artifactId: String,
  version: String,
  isCrossBuilt: Boolean
) {

  def toLibraryForm(
    resolvers: Seq[Resolver],
    crossBuildVersion: Option[Version]
  ): LibraryForm = {
    LibraryForm(
      resolvers = Resolvers.all(resolvers.map(_.uri)),
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

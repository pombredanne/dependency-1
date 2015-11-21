package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.LibraryForm

case class Artifact(
  groupId: String,
  artifactId: String,
  version: String
) {

  def toLibraryForm(resolvers: Seq[Resolver]): LibraryForm = {
    LibraryForm(
      resolvers = resolvers.map(_.uri),
      groupId = groupId,
      artifactId = artifactId,
      version = version
    )
  }

}

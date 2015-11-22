package com.bryzek.dependency.lib

case class ArtifactVersion(
  tag: VersionTag,
  crossBuildVersion: Option[VersionTag]
)


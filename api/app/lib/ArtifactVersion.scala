package com.bryzek.dependency.lib

case class ArtifactVersion(
  tag: Version,
  crossBuildVersion: Option[Version]
)


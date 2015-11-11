package com.bryzek.dependency.lib

case class Artifact(
  groupId: String,
  artifactId: String,
  version: Option[String] = None // TODO: Should we remove?
)

package com.bryzek.dependency.lib

import scala.concurrent.Future

trait VersionProvider {

  def latestVersion(
    groupId: String,
    artifactId: String
  ): Future[Option[String]]

}

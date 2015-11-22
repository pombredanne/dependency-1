package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.Library
import io.flow.play.util.Config

import scala.util.{Failure, Success, Try}
import java.net.URI

trait LibraryArtifactProvider {

  /**
    * Returns the artifacts for this library.
    */
  def artifacts(library: Library): Seq[ArtifactVersion]

}

case class DefaultLibraryArtifactProvider() extends LibraryArtifactProvider {


  override def artifacts(
    library: Library
  ) : Seq[ArtifactVersion] = {
    library.resolvers.map { resolver =>
      fetchArtifactVersions(resolver, library)
    }.flatten.distinct
  }

  private[this] def fetchArtifactVersions(
    resolver: String,
    library: Library
  ): Seq[ArtifactVersion] = {
    RemoteVersions.fetch(
      resolver = resolver,
      groupId = library.groupId,
      artifactId = library.artifactId
    )
  }

}

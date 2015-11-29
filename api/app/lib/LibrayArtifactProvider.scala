package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{Library, Resolver}
import io.flow.play.util.Config
import io.flow.user.v0.models.User

import scala.util.{Failure, Success, Try}
import java.net.URI

trait LibraryArtifactProvider {

  /**
    * Returns the artifacts for this library.
    * 
    * @param resolvers User specific resolvers. If provided, we check these first.
    */
  def artifacts(
    library: Library,
    resolvers: Seq[Resolver]
  ): Seq[ArtifactVersion]

}

case class DefaultLibraryArtifactProvider() extends LibraryArtifactProvider {

  override def artifacts(
    library: Library,
    resolvers: Seq[Resolver]
  ) : Seq[ArtifactVersion] = {
    Resolvers.all(resolvers.map(_.uri)).map { resolver =>
      RemoteVersions.fetch(
        resolver = resolver,
        groupId = library.groupId,
        artifactId = library.artifactId
      )
    }.flatten.distinct
  }

}

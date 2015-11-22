package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.Library
import io.flow.play.util.Config

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import java.net.URI

trait LibraryArtifactProvider {

  /**
    * Returns the artifacts for this library.
    */
  def artifacts(library: Library)(implicit ec: ExecutionContext): Future[Seq[ArtifactVersion]]

}

private[lib] case class DefaultLibraryArtifactProvider() extends LibraryArtifactProvider {


  override def artifacts(
    library: Library
  ) (
    implicit ec: ExecutionContext
  ) : Future[Seq[ArtifactVersion]] = {
    val versions = library.resolvers.map { resolver =>
      fetchArtifactVersions(resolver, library)
    }.flatten.distinct

    Future {
      versions
    }
  }

  private[this] def fetchArtifactVersions(
    resolver: String,
    library: Library
  ): Seq[ArtifactVersion] = {
    println(s"Resolver[$resolver]")
    val versions = RemoteVersions.fetch(
      resolver = resolver,
      groupId = library.groupId,
      artifactId = library.artifactId
    )
    println(s"Versions for groupId[${library.groupId}] artifactId[${library.artifactId}]")
    versions.foreach { v =>
      println(s"  - $v")
    }
    Nil
  }

}

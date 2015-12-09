package com.bryzek.dependency.lib

import db.{Authorization, ResolversDao}
import com.bryzek.dependency.v0.models.{Library, ResolverSummary}
import io.flow.play.util.Config
import io.flow.play.postgresql.Pager
import io.flow.user.v0.models.User

import scala.util.{Failure, Success, Try}
import java.net.URI

case class ArtifactResolution(
  resolver: ResolverSummary,
  versions: Seq[ArtifactVersion]
) {
  assert(!versions.isEmpty, "Must have at least one version")
}

trait LibraryArtifactProvider {

  /**
    * Returns the artifacts for this library.
    * 
    * @param user Used to look up private resolvers for this user.
    * @param resolver If specified, we search this resolver first
    */
  def artifacts(
    library: Library,
    user: Option[User],
    resolver: Option[ResolverSummary]
  ): Option[ArtifactResolution]

}

case class DefaultLibraryArtifactProvider() extends LibraryArtifactProvider {

  override def artifacts(
    library: Library,
    user: Option[User],
    resolver: Option[ResolverSummary]
  ): Option[ArtifactResolution] = {
    resolver.flatMap { r => ResolversDao.findByGuid(Authorization.All, r.guid) }.map { r =>
      RemoteVersions.fetch(
        resolver = r.uri,
        groupId = library.groupId,
        artifactId = library.artifactId,
        credentials = ResolversDao.credentials(r)
      )
    }.getOrElse(Nil) match {
      case Nil => {
        internalArtifacts(
          library = library,
          user = user,
          limit = 100,
          offset = 0
        )
      }
      case versions => {
        Some(ArtifactResolution(resolver.get, versions))
      }
    }
  }

  private[this] def internalArtifacts(
    library: Library,
    user: Option[User],
    limit: Long,
    offset: Long
  ): Option[ArtifactResolution] = {
    ResolversDao.findAll(
      Authorization(userGuid = user.map(_.guid)),
      userGuid = user.map(_.guid),
      limit = limit,
      offset = offset
    ) match {
      case Nil => {
        None
      }
      case resolvers => {
         resolvers.foreach { resolver =>
          RemoteVersions.fetch(
            resolver = resolver.uri,
            groupId = library.groupId,
            artifactId = library.artifactId,
            credentials = ResolversDao.credentials(resolver)
          ) match {
            case Nil => {}
            case versions => {
              return Some(
                ArtifactResolution(
                  ResolverSummary(
                    guid = resolver.guid,
                    visibility = resolver.visibility,
                    uri = resolver.uri
                  ),
                  versions
                )
              )
            }
          }
        }

        internalArtifacts(
          library = library,
          user = user,
          limit = limit,
          offset = offset + limit
        )
      }
    }
  }

}

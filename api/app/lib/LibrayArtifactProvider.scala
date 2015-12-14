package com.bryzek.dependency.api.lib

import db.{Authorization, ResolversDao}
import com.bryzek.dependency.v0.models.{Library, OrganizationSummary, ResolverSummary}
import io.flow.play.util.Config
import io.flow.play.postgresql.Pager

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
    * @param organization Used to look up private resolvers for this organization.
    * @param resolver If specified, we search this resolver first
    */
  def artifacts(
    library: Library,
    organization: OrganizationSummary,
    resolver: Option[ResolverSummary]
  ): Option[ArtifactResolution]

}

case class DefaultLibraryArtifactProvider() extends LibraryArtifactProvider {

  override def artifacts(
    library: Library,
    organization: OrganizationSummary,
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
          organization = organization,
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
    organization: OrganizationSummary,
    limit: Long,
    offset: Long
  ): Option[ArtifactResolution] = {
    ResolversDao.findAll(
      Authorization.Organization(organization.guid),
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
          organization = organization,
          limit = limit,
          offset = offset + limit
        )
      }
    }
  }

}

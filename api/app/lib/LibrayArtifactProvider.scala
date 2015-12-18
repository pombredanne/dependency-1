package com.bryzek.dependency.api.lib

import db.{Authorization, ResolversDao}
import com.bryzek.dependency.v0.models.{OrganizationSummary, ResolverSummary, Visibility}

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
    organization: OrganizationSummary,
    groupId: String,
    artifactId: String,
    resolver: Option[ResolverSummary]
  ): Option[ArtifactResolution]

}

case class DefaultLibraryArtifactProvider() extends LibraryArtifactProvider {

  override def artifacts(
    organization: OrganizationSummary,
    groupId: String,
    artifactId: String,
    resolver: Option[ResolverSummary]
  ): Option[ArtifactResolution] = {
    resolver.flatMap { r => ResolversDao.findByGuid(Authorization.All, r.guid) }.map { r =>
      RemoteVersions.fetch(
        resolver = r.uri,
        groupId = groupId,
        artifactId = artifactId,
        credentials = ResolversDao.credentials(r)
      )
    }.getOrElse(Nil) match {
      case Nil => {
        internalArtifacts(
          organization = organization,
          groupId = groupId,
          artifactId = artifactId,
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
    organization: OrganizationSummary,
    groupId: String,
    artifactId: String,
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
            groupId = groupId,
            artifactId = artifactId,
            credentials = ResolversDao.credentials(resolver)
          ) match {
            case Nil => {}
            case versions => {
              return Some(
                ArtifactResolution(
                  ResolverSummary(
                    guid = resolver.guid,
                    organization = resolver.visibility match {
                      case Visibility.Public => None
                      case Visibility.Private | Visibility.UNDEFINED(_) => Some(organization)
                    },
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
          organization = organization,
          groupId = groupId,
          artifactId = artifactId,
          limit = limit,
          offset = offset + limit
        )
      }
    }
  }

}

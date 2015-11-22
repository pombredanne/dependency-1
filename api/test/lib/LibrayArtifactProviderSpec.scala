package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.Library
import io.flow.common.v0.models.{Audit, Reference}
import org.joda.time.DateTime
import java.util.UUID

import play.api.libs.ws._
import play.api.test._

class LibrayArtifactProviderSpec extends PlaySpecification {

  def createAudit(): Audit = {
    val reference = Reference(UUID.randomUUID())
    val now = new DateTime()
    Audit(
      createdAt = now,
      createdBy = reference,
      updatedAt = now,
      updatedBy = reference
    )
  }

  def createLibrary(
    resolvers: Seq[String] = Resolvers.Default,
    groupId: String = UUID.randomUUID.toString,
    artifactId: String = UUID.randomUUID.toString
  ): Library = {
    Library(
      guid = UUID.randomUUID,
      resolvers = resolvers,
      groupId = groupId,
      artifactId = artifactId,
      audit = createAudit()
    )
  }

  lazy val provider = DefaultLibraryArtifactProvider()

  "parseUri" in {
    val library = createLibrary(groupId = "com.github.tototoshi", artifactId = "scala-csv")
    val versions = provider.artifacts(library)
    versions.find { v =>
      v.tag.version == "1.2.2" && v.crossBuildVersion.map(_.version) == Some("2.11")
    }.map(_.tag.version) must beEqualTo(Some("1.2.2"))
  }

}

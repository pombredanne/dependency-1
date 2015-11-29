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
    groupId: String = UUID.randomUUID.toString,
    artifactId: String = UUID.randomUUID.toString
  ): Library = {
    Library(
      guid = UUID.randomUUID,
      groupId = groupId,
      artifactId = artifactId,
      audit = createAudit()
    )
  }

  lazy val provider = DefaultLibraryArtifactProvider()

  "parseUri" in {
    val library = createLibrary(groupId = "com.github.tototoshi", artifactId = "scala-csv")
    val versions = provider.artifacts(library, resolvers = Nil)
    versions.find { v =>
      v.tag.value == "1.2.2" && v.crossBuildVersion.map(_.value) == Some("2.11")
    }.map(_.tag.value) must beEqualTo(Some("1.2.2"))
  }

  "swagger" in {
    val library = createLibrary(groupId = "io.swagger", artifactId = "swagger-parser")
    val versions = provider.artifacts(library, resolvers = Nil).map(_.tag.value)
    println(s"versions: " + versions.mkString(", "))
    versions.contains("1.0.4") must beTrue
    versions.contains("1.0.13") must beTrue
    versions.contains("0.0.139") must beFalse
  }

}

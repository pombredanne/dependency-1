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

  // See https://github.com/sbt/sbt/blob/3e5449f02d082c1326da7e6319d70d5b26b84bfd/launch/src/main/resources/sbt.boot.properties0.11.3
  val DefaultResolvers = Seq(
    "http://jcenter.bintray.com/",
    "http://repo.typesafe.com/typesafe/ivy-releases/",
    "http://oss.sonatype.org/content/repositories/snapshots",
    "http://repo1.maven.org/maven2/"
  )

  def createLibrary(
    resolvers: Seq[String] = DefaultResolvers,
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
    provider.artifacts(library) must beEqualTo(Nil)
  }

}

package com.bryzek.dependency.lib

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._

import com.bryzek.dependency.v0.models.Library
import io.flow.common.v0.models.{Audit, Reference}
import io.flow.play.clients.MockUserClient
import org.joda.time.DateTime
import java.util.UUID

class LibraryArtifactProviderSpec extends PlaySpec with OneAppPerSuite {

  def createLibrary(
    groupId: String = UUID.randomUUID.toString,
    artifactId: String = UUID.randomUUID.toString
  ): Library = {
    Library(
      guid = UUID.randomUUID,
      groupId = groupId,
      artifactId = artifactId,
      audit = MockUserClient.makeAudit()
    )
  }

  lazy val provider = DefaultLibraryArtifactProvider()

  "parseUri" in {
    val library = createLibrary(groupId = "com.github.tototoshi", artifactId = "scala-csv")
    val resolution = provider.artifacts(library, user = None, resolver = None).getOrElse {
      sys.error("Could not find scala-csv library")
    }
    resolution.versions.find { v =>
      v.tag.value == "1.2.2" && v.crossBuildVersion.map(_.value) == Some("2.11")
    }.map(_.tag.value) must be(Some("1.2.2"))
  }

  "swagger" in {
    val library = createLibrary(groupId = "io.swagger", artifactId = "swagger-parser")
    val resolution = provider.artifacts(library, user = None, resolver = None).getOrElse {
      sys.error("Could not find swagger-parser library")
    }
    val tags = resolution.versions.map(_.tag.value)
    tags.contains("1.0.4") must be(true)
    tags.contains("1.0.13") must be(true)
    tags.contains("0.0.139") must be(false)
  }

}

package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{Language, Library}
import io.flow.github.v0.Client
import io.flow.github.v0.errors.UnitResponse
import io.flow.github.v0.models.{Contents, Encoding}
import org.apache.commons.codec.binary.Base64

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class RepositoryMetadata(
  languages: Seq[Language],
  libraries: Seq[Library]
)

object GitHub {

  def toText(contents: Contents): String = {
    (contents.content, contents.encoding) match {
      case (Some(encoded), Encoding.Base64) => {
        new String(Base64.decodeBase64(encoded.getBytes))
      }
      case (Some(_), Encoding.UNDEFINED(name)) => {
        sys.error(s"Unsupported encoding[$name] for content: $contents")
      }
      case (None, _) => {
        sys.error(s"No contents for: $contents")
      }
    }
  }

}

class GitHub @Inject() (
  client: Client
) {

  def repositoryMetadata(
    owner: String,
    repository: String
  ) (
    implicit ec: ExecutionContext
  ) : Future[Option[RepositoryMetadata]] = {
    client.contents.getReposByOwnerAndRepoAndPath(
      owner = owner,
      repo = repository,
      path = "build.sbt"
    ).map { contents =>
      val result = ParseBuildSbt(
        GitHub.toText(contents)
      )
      Some(
        RepositoryMetadata(
          languages = result.languages,
          libraries = result.libraries
        )
      )
    }.recover {
      case UnitResponse(404) => None
    }
  }

}

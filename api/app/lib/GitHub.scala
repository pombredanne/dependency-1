package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{LanguageForm, LibraryForm}
import io.flow.github.v0.Client
import io.flow.github.v0.errors.UnitResponse
import io.flow.github.v0.models.{Contents, Encoding}
import org.apache.commons.codec.binary.Base64

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class RepositoryMetadata(
  languages: Seq[LanguageForm],
  libraries: Seq[LibraryForm]
)

object GitHub {

  case class FullName(owner: String, repository: String)

  def parseFullName(fullName: String): Either[String, FullName] = {
    fullName.split("/").toList match {
      case Nil => Left(s"Invalid full name[$fullName]")
      case owner :: Nil => Left(s"Invalid full name[$fullName] - missing /")
      case owner :: repo :: Nil => Right(FullName(owner, repo))
      case multiple => Left(s"Invalid full name[$fullName] - expected exactly one /")
    }
  }

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

case class GithubClientRepositoryMetadataFetcher(githubToken: String) {

  private val GithubHost = "https://api.github.com"
  private val Filename = "build.sbt"

  lazy val client = new Client(
    apiUrl = GithubHost,
    defaultHeaders = Seq(
      "Authorization" -> s"token $githubToken"
    )
  )

  def repositoryMetadata(
    fullName: String
  ) (
    implicit ec: ExecutionContext
  ) : Future[Option[RepositoryMetadata]] = {
    GitHub.parseFullName(fullName) match {
      case Left(error) => {
        sys.error(error)
      }
      case Right(parsed) => {
        client.contents.getReposByOwnerAndRepoAndPath(
          owner = parsed.owner,
          repo = parsed.repository,
          path = Filename
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
  }

}

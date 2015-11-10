package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{LanguageForm, LibraryForm, Project}
import io.flow.github.v0.Client
import io.flow.github.v0.errors.UnitResponse
import io.flow.github.v0.models.{Contents, Encoding}
import io.flow.play.util.Config
import org.apache.commons.codec.binary.Base64

import scala.concurrent.{ExecutionContext, Future}

object GitHubClient {

  private lazy val token: String = {
    (
      Config.optionalString("github.api.token.file"),
      Config.optionalString("github.api.token")
    ) match {
      case (None, None) => sys.error("Missing configuration for github.api.token and github.api.token.file")
      case (Some(_), Some(_)) => sys.error("Cannot specify configuration for both github.api.token and github.api.token.file")
      case (Some(file), None) => scala.io.Source.fromFile("/tmp/github-token.txt", "UTF-8").mkString
      case (None, Some(token)) => token
    }
  }

  lazy val instance = new GitHubDependencyProvider(token)

}

object GitHubUtil {

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

private[lib] case class GitHubDependencyProvider(githubToken: String) extends DependencyProvider {

  private val GithubHost = "https://api.github.com"
  private val Filename = "build.sbt"

  lazy val client = new Client(
    apiUrl = GithubHost,
    defaultHeaders = Seq(
      "Authorization" -> s"token $githubToken"
    )
  )

  override def dependencies(project: Project)(implicit ec: ExecutionContext): Future[Option[Dependencies]] = {
    GitHubUtil.parseFullName(project.name) match {
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
            GitHubUtil.toText(contents)
          )
          Some(
            Dependencies(
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

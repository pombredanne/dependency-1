package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{LanguageForm, LibraryForm, Project}
import io.flow.github.v0.Client
import io.flow.github.v0.errors.UnitResponse
import io.flow.github.v0.models.{Contents, Encoding}
import io.flow.play.util.DefaultConfig
import org.apache.commons.codec.binary.Base64

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import java.net.URI

object GitHubClient {

  private lazy val token: String = {
    (
      DefaultConfig.optionalString("github.api.token.file"),
      DefaultConfig.optionalString("github.api.token.value")
    ) match {
      case (None, None) => sys.error("Missing configuration for github.api.token and github.api.token.file")
      case (Some(_), Some(_)) => sys.error("Cannot specify configuration for both github.api.token and github.api.token.file")
      case (Some(file), None) => scala.io.Source.fromFile("/tmp/github-token.txt", "UTF-8").mkString.trim
      case (None, Some(token)) => token
    }
  }

  lazy val instance = new GitHubDependencyProvider(token)

}

object GitHubUtil {

  case class Repository(
    owner: String,
    project: String
  )
  
  def parseUri(uri: String): Either[String, Repository] = {
    uri.trim match {
      case "" => Left(s"URI cannot be an empty string")
      case trimmed => {
        Try(URI.create(trimmed)) match {
          case Failure(error) => Left(s"Could not parse uri[$trimmed]: $error")
          case Success(u) => {
            val path = if (u.getPath.startsWith("/")) {
              u.getPath.substring(1)
            } else {
              u.getPath
            }.trim
            path.split("/").filter(!_.isEmpty).toList match {
              case Nil => Left(s"URI path cannot be empty for uri[$trimmed]")
              case owner :: Nil => Left(s"Invalid uri path[$trimmed] missing project name")
              case owner :: project :: Nil => Right(Repository(owner, project))
              case multiple => Left(s"Invalid uri path[$u] - expected exactly two path components")
            }
          }
        }
      }
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
  private val BuildSbtFilename = "build.sbt"
  private val ProjectPluginsSbtFilename = "project/plugins.sbt"

  lazy val client = new Client(
    apiUrl = GithubHost,
    defaultHeaders = Seq(
      "Authorization" -> s"token $githubToken"
    )
  )

  override def dependencies(project: Project)(implicit ec: ExecutionContext): Future[Option[Dependencies]] = {
    GitHubUtil.parseUri(project.uri) match {
      case Left(error) => {
        sys.error(error)
      }
      case Right(repo) => {
        for {
          build <- getBuildDependencies(repo)
          plugins <- getPluginsDependencies(repo)
        } yield {
          (build, plugins) match {
            case (None, None) => None
            case (Some(build), None) => Some(build)
            case (None, Some(plugins)) => Some(plugins)
            case (Some(build), Some(plugins)) => {
              Some(
                build.copy(
                  resolvers = plugins.resolvers,
                  plugins = plugins.plugins
                )
              )
            }
          }
        }
      }
    }
  }

  private[this] def getBuildDependencies(
    repo: GitHubUtil.Repository
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[Dependencies]] = {
    client.contents.getReposByOwnerAndRepoAndPath(
      owner = repo.owner,
      repo = repo.project,
      path = BuildSbtFilename
    ).map { contents =>
      val result = BuildSbtScalaParser(
        GitHubUtil.toText(contents)
      )
      Some(
        Dependencies(
          languages = Some(result.languages),
          libraries = Some(result.libraries)
        )
      )
    }.recover {
      case UnitResponse(404) => None
    }
  }

  private[this] def getPluginsDependencies(
    repo: GitHubUtil.Repository
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[Dependencies]] = {
    client.contents.getReposByOwnerAndRepoAndPath(
      owner = repo.owner,
      repo = repo.project,
      path = ProjectPluginsSbtFilename
    ).map { contents =>
      val result = ProjectPluginsSbtScalaParser(
        GitHubUtil.toText(contents)
      )
      Some(
        Dependencies(
          plugins = Some(result.plugins),
          resolvers = Some(result.resolvers)
        )
      )
    }.recover {
      case UnitResponse(404) => None
    }
  }

}

package com.bryzek.dependency.lib

import io.flow.user.v0.models.User
import com.bryzek.dependency.v0.models.{LanguageForm, LibraryForm, Project}
import io.flow.github.v0.Client
import io.flow.github.v0.errors.UnitResponse
import io.flow.github.v0.models.{Contents, Encoding}
import io.flow.play.util.DefaultConfig
import org.apache.commons.codec.binary.Base64

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import java.net.URI

object GithubUtil {

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

object GithubDependencyProviderClient {

  def instance(user: User) = {
    new GithubDependencyProvider(new DefaultGithub(), user)
  }

}

private[lib] case class GithubDependencyProvider(
  github: Github,
  user: User
) extends DependencyProvider {

  private val BuildSbtFilename = "build.sbt"
  private val ProjectPluginsSbtFilename = "project/plugins.sbt"

  override def dependencies(project: Project)(implicit ec: ExecutionContext): Future[Option[Dependencies]] = {
    for {
      build <- getBuildDependencies(project.uri)
      plugins <- getPluginsDependencies(project.uri)
    } yield {
      (build, plugins) match {
        case (None, None) => None
        case (Some(build), None) => Some(build)
        case (None, Some(plugins)) => Some(plugins)
        case (Some(build), Some(plugins)) => {
          Some(
            build.copy(
              resolverUris = Some((plugins.resolverUris.getOrElse(Nil) ++ build.resolverUris.getOrElse(Nil)).distinct),
              plugins = Some((plugins.plugins.getOrElse(Nil) ++ build.plugins.getOrElse(Nil)).distinct)
            )
          )
        }
      }
    }
  }

  private[this] def getBuildDependencies(
    projectUri: String
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[Dependencies]] = {
    github.file(user, projectUri, BuildSbtFilename).map { result =>
      result.flatMap { text =>
        val result = BuildSbtScalaParser(text)
        Some(
          Dependencies(
            languages = Some(result.languages),
            libraries = Some(result.libraries),
            resolverUris = Some(result.resolverUris)
          )
        )
      }
    }
  }

  private[this] def getPluginsDependencies(
    projectUri: String
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[Dependencies]] = {
    github.file(user, projectUri, BuildSbtFilename).map { result =>
      result.flatMap { text =>
        val result = ProjectPluginsSbtScalaParser(text)
        Some(
          Dependencies(
            plugins = Some(result.plugins),
            resolverUris = Some(result.resolverUris)
          )
        )
      }
    }
  }

}

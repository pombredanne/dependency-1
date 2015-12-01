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
  private val BuildPropertiesFilename = "project/build.properties"

  override def dependencies(project: Project)(implicit ec: ExecutionContext): Future[Dependencies] = {
    for {
      build <- getBuildDependencies(project.uri)
      plugins <- getPluginsDependencies(project.uri)
      properties <- parseProperties(project.uri)
    } yield {
      Seq(build, plugins, properties).flatten.foldLeft(Dependencies()) { case (all, dep) =>
        all.copy(
          resolverUris = Some((all.resolverUris.getOrElse(Nil) ++ dep.resolverUris.getOrElse(Nil)).distinct),
          plugins = Some((all.plugins.getOrElse(Nil) ++ dep.plugins.getOrElse(Nil)).distinct),
          languages = Some((all.languages.getOrElse(Nil) ++ dep.languages.getOrElse(Nil)).distinct)
        )
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
        val result = BuildSbtScalaParser(
          description = s"Project[${projectUri}] file[$BuildSbtFilename]",
          contents = text
        )
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

  private[this] def parseProperties(
    projectUri: String
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[Dependencies]] = {
    github.file(user, projectUri, BuildPropertiesFilename).map { result =>
      result.flatMap { text =>
        val properties = PropertiesParser(
          description = s"Project[${projectUri}] file[$BuildPropertiesFilename]",
          contents = text
        )
        properties.get("sbt.version").map { value =>
          Dependencies(
            Some(
              Seq(
                LanguageForm(
                  name = "sbt",
                  version = value
                )
              )
            )
          )
        }
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
        val result = ProjectPluginsSbtScalaParser(
          description = s"Project[${projectUri}] file[$BuildPropertiesFilename]",
          contents = text
        )

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

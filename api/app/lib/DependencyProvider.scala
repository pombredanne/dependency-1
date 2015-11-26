package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{LanguageForm, LibraryForm, Project}
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}

case class Dependencies(
  languages: Option[Seq[LanguageForm]] = None,
  libraries: Option[Seq[Artifact]] = None,
  resolvers: Option[Seq[Resolver]] = None,
  plugins: Option[Seq[Artifact]] = None
) {

  def librariesAndPlugins: Option[Seq[Artifact]] = {
    (libraries, plugins) match {
      case (None, None) => None
      case (Some(lib), None) => Some(lib)
      case (None, Some(plugins)) => Some(plugins)
      case (Some(lib), Some(plugins)) => Some(lib ++ plugins)
    }
  }

  lazy val crossBuildVersion: Option[Version] = {
    languages match {
      case None => None
      case Some(langs) => {
        langs.map(_.version).distinct.map(Version(_)).sorted.reverse.toList match {
          case Nil => None
          case one :: Nil => {
            Some(one)
          }
          case multiple => {
            Logger.warn(s"Found multiple language versions[${multiple.mkString(", ")}]. Using first")
            multiple.headOption
          }
        }
      }
    }
  }

}


trait DependencyProvider {

  /**
    * Returns the dependencies for this project. If you return None,
    * you are indicating that the project was not found or does not
    * exist.
    */
  def dependencies(project: Project)(implicit ec: ExecutionContext): Future[Option[Dependencies]]

}

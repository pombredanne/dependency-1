package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{LanguageForm, LibraryForm, Project}
import scala.concurrent.{ExecutionContext, Future}

case class Dependencies(
  languages: Option[Seq[LanguageForm]] = None,
  libraries: Option[Seq[LibraryForm]] = None,
  resolvers: Option[Seq[Resolver]] = None,
  plugins: Option[Seq[LibraryForm]] = None
) {

  def librariesAndPlugins: Option[Seq[LibraryForm]] = {
    (libraries, plugins) match {
      case (None, None) => None
      case (Some(lib), None) => Some(lib)
      case (None, Some(plugins)) => Some(plugins)
      case (Some(lib), Some(plugins)) => Some(lib ++ plugins)
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

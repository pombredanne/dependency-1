package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{LanguageForm, LibraryForm, Project}
import scala.concurrent.{ExecutionContext, Future}

case class Dependencies(
  languages: Seq[LanguageForm],
  libraries: Seq[LibraryForm]
)

trait DependencyProvider {

  /**
    * Returns the dependencies for this project. If you return None,
    * you are indicating that the project was not found or does not
    * exist.
    */
  def dependencies(project: Project)(implicit ec: ExecutionContext): Future[Option[Dependencies]]

}

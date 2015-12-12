package com.bryzek.dependency.api.lib

import com.bryzek.dependency.v0.models.{BinaryForm, LibraryForm, BinaryType, Project}
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}

case class Dependencies(
  binaries: Option[Seq[BinaryForm]] = None,
  libraries: Option[Seq[Artifact]] = None,
  resolverUris: Option[Seq[String]] = None,
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

  def crossBuildVersion(): Option[Version] = {
    binaries match {
      case None => None
      case Some(langs) => {
        langs.sortBy { l => Version(l.version) }.reverse.find(_.name == BinaryType.Scala.toString).headOption.map { lang =>
          DependencyHelper.crossBuildVersion(lang)
        }
      }
    }
  }

}

private[lib] object DependencyHelper {

  def crossBuildVersion(lang: BinaryForm): Version = {
    val version = Version(lang.version)
    BinaryType(lang.name) match {
      case BinaryType.Scala |  BinaryType.Sbt=> {
        version.tags.head match {
          case Tag.Semver(major, minor, _, _) => {
            // This is most common. We just want major and minor
            // version - e.g. 2.11.7 becomes 2.11.
            Version(s"${major}.${minor}", Seq(Tag.Semver(major, minor, 0)))
          }
          case _ => version
        }
      }
      case BinaryType.UNDEFINED(_) => {
        version
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
  def dependencies(project: Project)(implicit ec: ExecutionContext): Future[Dependencies]

}

package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.ProgrammingLanguage
import org.apache.commons.lang3.StringUtils
import play.api.Logger


trait LanguageVersionProvider {

  /**
    * Returns the versions for this language, fetching them from
    * appropriate remote locations.
    */
  def versions(language: ProgrammingLanguage): Seq[Version]

}

object DefaultLanguageVersionProvider extends LanguageVersionProvider {

  private[this] val ScalaUrl = "http://www.scala-lang.org/download/all.html"
  private[this] val SbtUrl = "https://dl.bintray.com/sbt/native-packages/sbt/"

  override def versions(
    language: ProgrammingLanguage
  ) : Seq[Version] = {
    language match {
      case ProgrammingLanguage.Scala => {
        fetchScalaVersions()
      }
      case ProgrammingLanguage.Sbt => {
        fetchSbtVersions()
      }
      case ProgrammingLanguage.UNDEFINED(name) => {
        Logger.warn(s"Do not know how to find versions for the programming language[$name]")
        Nil
      }
    }
  }

  def fetchScalaVersions(): Seq[Version] = {
    RemoteDirectory.fetch(ScalaUrl) { name =>
      name.toLowerCase.startsWith("scala ")
    }.files.flatMap { toVersion(_) }
  }

  def fetchSbtVersions(): Seq[Version] = {
    RemoteDirectory.fetch(SbtUrl)().directories.flatMap { dir =>
      toVersion(StringUtils.stripEnd(dir, "/"))
    }
  }

  def toVersion(value: String): Option[Version] = {
    val tag = Version(
      StringUtils.stripStart(
        StringUtils.stripStart(value, "scala"),
        "Scala"
      ).trim
    )
    tag.major match {
      case None => None
      case Some(_) => Some(tag)
    }
  }

}

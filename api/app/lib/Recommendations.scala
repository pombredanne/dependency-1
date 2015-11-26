package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.VersionForm

object Recommendations {

  /**
    * Given the current version and a list of possible versions,
    * suggests the best version to which to upgrade (or None if not
    * found)
    */
  def version(current: VersionForm, others: Seq[VersionForm]): Option[String] = {
    val currentTag = Version(current.version)

    others.
      filter(_ != current).
      filter(_.crossBuildVersion == current.crossBuildVersion).
      map(v => Version(v.version)).
      filter(_ > currentTag).
      filter(textPortionsMatch(currentTag, _)).
      sorted.
      reverse.
      headOption.
      map(_.value)
  }

  private[this] def textPortionsMatch(current: Version, other: Version): Boolean = {
    (current.tags zip other.tags).map{ case (a, b) =>
      (a, b) match {
        case (t1: Tag.Semver, t2: Tag.Semver) => true
        case (t1: Tag.Date, t2: Tag.Date) => true
        case (Tag.Text(value1), Tag.Text(value2)) => value1 == value2
        case (_, _) => false
      }
    }.forall( el => el )
  }

}

package com.bryzek.dependency.lib

object Recommendations {

  /**
    * Given the current version and a list of possible versions,
    * suggests the best version to which to upgrade (or None if not
    * found)
    */
  def version(current: String, others: Seq[String]): Option[String] = {
    val currentTag = Version(current)
    others.
      filter(_ != current).
      map(Version(_)).
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

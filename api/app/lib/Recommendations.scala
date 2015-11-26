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
    println(s"current: ${current}")
    println(s"other: $other")
    true
  }

}

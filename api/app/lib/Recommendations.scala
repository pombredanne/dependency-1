package com.bryzek.dependency.lib

object Recommendations {

  /**
    * Given the current version and a list of possible versions,
    * suggests the best version to which to upgrade (or None if not
    * found)
    */
  def version(current: String, others: Seq[String]): Option[String] = {
    others.
      filter(_ != current).
      map(VersionTag(_)).
      filter(_ > VersionTag(current)).
      filter(_.qualifier == VersionTag(current).qualifier).
      sorted.
      reverse.
      headOption.
      map(_.version)
  }

}

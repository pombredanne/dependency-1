package com.bryzek.dependency.lib

sealed trait VersionTag2 extends Ordered[VersionTag2] {

  /**
    * The original version number
    */
  val version: String

  /**
    * Lexicographic sort key
    */
  val sortKey: String

  /**
    * If present, the major version number
    */
  val major: Option[Int]

  /**
    * If present, the qualifier on the version number (everything
    * after the semver portion)
    */
  val qualifier: Option[String]

  /**
    * If possible, computes the next micro version
    */
  def nextMicro(): Option[String]

  def compare(that: VersionTag2) = {
    sortKey.compare(that.sortKey)
  }

}

object VersionTag2 {

  private[this] val Divider = "|"
  val Padding = 10000
  val MinPadding = 0
  val MaxPadding = 99999

  private[lib] val SemverRx = """^[a-z]?([\d\.]+)$""".r

  def apply(value: String): VersionTag2 = {
    fromString(value).getOrElse {
      Unknown(value)
    }
  }

  def fromString(value: String): Option[VersionTag2] = {
    value.trim.split(VersionTag.Dash).flatMap(versionTagFromString(_)).toList match {
      case Nil => None
      case a :: Nil => Some(a)
      case a :: b :: Nil => {
        (a, b) match {
          case (a: Semver, b: Unknown) => Some(QualifiedSemver(a.majorNum, a.minorNum, a.microNum, b.version))
          case _ => Some(Multi(Seq(a, b)))
        }
      }
      case multiple => Some(Multi(multiple))
    }
  }

  private def versionTagFromString(value: String): Option[VersionTag2] = {
    println(s"versionTagFromString($value)")
    value.trim match {
      case SemverRx(rest) => {
        value.split(VersionTag.Dot).map(_.toInt).toList match {
          case Nil => None
          case major :: Nil => Some(Semver(major, 0, 0))
          case major :: minor :: Nil => Some(Semver(major, minor, 0))
          case major :: minor :: micro :: Nil => Some(Semver(major, minor, micro))
          case major :: minor :: micro :: rest => {
            Some(QualifiedSemver(major, minor, micro, rest.mkString(".")))
          }
        }
      }
      case _ => Some(Unknown(value.trim))
    }
  }

  case class Semver(majorNum: Int, minorNum: Int, microNum: Int) extends VersionTag2 {
    override val version = Seq(majorNum, minorNum, microNum).mkString(VersionTag.Dot)
    override val sortKey = Seq(Padding + majorNum, Padding + minorNum, Padding + microNum).mkString(Divider)
    override val major = Some(majorNum)
    override val qualifier = None
    override def nextMicro() = Some(Seq(majorNum, minorNum, microNum + 1).mkString(VersionTag.Dot))
  }

  case class QualifiedSemver(majorNum: Int, minorNum: Int, microNum: Int, qual: String) extends VersionTag2 {
    override val version = Seq(majorNum, minorNum, microNum, qual).mkString(VersionTag.Dot) + s"-$qual"
    override val sortKey = Seq(Padding + majorNum, Padding + minorNum, Padding + microNum, MaxPadding, qual).mkString(Divider)
    override val major = Some(majorNum)
    override val qualifier = Some(qual)
    override def nextMicro() = Some(Seq(majorNum, minorNum, microNum + 1).mkString(VersionTag.Dot) + s"-$qual")
  }

  case class Unknown(tag: String) extends VersionTag2 {
    override val version = tag
    override val sortKey: String = QualifiedSemver(0, 0, 0, tag).sortKey
    override val major = None
    override val qualifier = None
    override def nextMicro() = None
  }

  case class Multi(tags: Seq[VersionTag2]) extends VersionTag2 {
    assert(tags.size > 1, "Must have at least two tags")
    override val version = tags.map(_.version).mkString(VersionTag.Dot)
    override val sortKey = tags.map(_.sortKey).mkString(Divider)
    override val major = tags.head.major
    override val qualifier = None
    override def nextMicro() = None
  }

}

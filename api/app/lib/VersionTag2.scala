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
  def nextMicro(): Option[VersionTag2]

  def compare(that: VersionTag2) = {
    sortKey.compare(that.sortKey)
  }

}

object VersionTag2 {

  private[this] val Divider = ":"
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
          case (a: Semver, b: Unknown) => Some(QualifiedSemver(value, a.majorNum, a.minorNum, a.microNum, b.version))
          case _ => Some(Multi(value, Seq(a, b)))
        }
      }
      case multiple => Some(Multi(value, multiple))
    }
  }

  private def versionTagFromString(value: String): Option[VersionTag2] = {
    value.trim match {
      case SemverRx(rest) => {
        rest.split(VersionTag.Dot).map(_.toInt).toList match {
          case Nil => None
          case major :: Nil => Some(Semver(value, major, 0, 0))
          case major :: minor :: Nil => Some(Semver(value, major, minor, 0))
          case major :: minor :: micro :: Nil => Some(Semver(value, major, minor, micro))
          case major :: minor :: micro :: rest => {
            Some(QualifiedSemver(value, major, minor, micro, rest.mkString(".")))
          }
        }
      }
      case _ => Some(Unknown(value.trim))
    }
  }

  /**
    * Ex: 1.5.0
    */
  case class Semver(version: String, majorNum: Int, minorNum: Int, microNum: Int) extends VersionTag2 {
    override val sortKey = Seq(5, Padding + majorNum, Padding + minorNum, Padding + microNum).mkString(Divider)
    override val major = Some(majorNum)
    override val qualifier = None
    override def nextMicro() = Some(Semver(s"${majorNum}.${minorNum}.${microNum+1}", majorNum, minorNum, microNum + 1))
  }

  /**
    * Ex: 20120809.1
    */
  case class Date(version: String, date: Long, minorNum: Int) extends VersionTag2 {
    override val sortKey = Seq(4, Padding + date, Padding + minorNum).mkString(Divider)
    override val major = None
    override val qualifier = None
    override def nextMicro() = {
      Some(Date(s"${date}.${minorNum + 1}", date, minorNum + 1))
    }
  }

  case class QualifiedSemver(version: String, majorNum: Int, minorNum: Int, microNum: Int, qual: String) extends VersionTag2 {
    override val sortKey = Seq(3, Padding + majorNum, Padding + minorNum, Padding + microNum, MaxPadding, qual).mkString(Divider)
    override val major = Some(majorNum)
    override val qualifier = Some(qual)
    override def nextMicro() = Some(QualifiedSemver(s"${majorNum}.${minorNum}.${microNum+1}-$qual", majorNum, minorNum, microNum + 1, qual))
  }

  case class Unknown(version: String) extends VersionTag2 {
    override val sortKey: String = Seq(1, Padding, version).mkString(Divider)
    override val major = None
    override val qualifier = None
    override def nextMicro() = None
  }

  case class Multi(version: String, tags: Seq[VersionTag2]) extends VersionTag2 {
    assert(tags.size > 1, "Must have at least two tags")
    override val sortKey = Seq(2, Padding, tags.map(_.sortKey).mkString("|")).mkString(Divider)
    override val major = tags.head.major
    override val qualifier = None
    override def nextMicro() = None
  }

}

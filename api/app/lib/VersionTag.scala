package com.bryzek.dependency.lib

sealed trait VersionTag extends Ordered[VersionTag] {

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
    * If possible, computes the next micro version
    */
  def nextMicro(): Option[VersionTag]

  def compare(that: VersionTag) = {
    sortKey.compare(that.sortKey)
  }

}

object VersionTag {

  private[this] val Divider = ":"
  val Dash = """\-"""
  val Dot = """\."""
  val Padding = 10000

  private[lib] val SemverRx = """^[a-z]?([\d\.]+)$""".r

  def apply(value: String): VersionTag = {
    value.trim.split(VersionTag.Dash).flatMap(fromString(_)).toList match {
      case Nil => Unknown(value)
      case one :: Nil => one
      case multiple => Multi(value, multiple)
    }
  }

  private def fromString(value: String): Option[VersionTag] = {
    value.trim match {
      case SemverRx(rest) => {
        rest.split(VersionTag.Dot).map(_.toInt).toList match {
          case Nil => None
          case major :: Nil => Some(Semver(value, major, 0, 0))
          case major :: minor :: Nil => {
            isDate(major) match {
              case true => Some(Date(value, major, minor))
              case false => Some(Semver(value, major, minor, 0))
            }
          }
          case major :: minor :: micro :: rest => {
            Some(Semver(value, major, minor, micro))
          }
        }
      }
      case _ => Some(Unknown(value.trim))
    }
  }

  private[lib] def isDate(value: Int): Boolean = {
    value.toString.length >= 8 && value.toString.substring(0, 4).toInt >= 1900
  }

  /**
    * Ex: 1.5.0
    */
  case class Semver(version: String, majorNum: Int, minorNum: Int, microNum: Int) extends VersionTag {
    override val sortKey = Seq(7, Padding + majorNum, Padding + minorNum, Padding + microNum).mkString(Divider)
    override val major = Some(majorNum)
    override def nextMicro() = Some(Semver(s"${majorNum}.${minorNum}.${microNum+1}", majorNum, minorNum, microNum + 1))
  }

  /**
    * Ex: 20120809.1
    */
  case class Date(version: String, date: Long, minorNum: Int) extends VersionTag {
    override val sortKey = Seq(5, Padding + date, Padding + minorNum, version.toLowerCase).mkString(Divider)
    override val major = None
    override def nextMicro() = {
      Some(Date(s"${date}.${minorNum + 1}", date, minorNum + 1))
    }
  }

  case class Multi(version: String, tags: Seq[VersionTag]) extends VersionTag {
    assert(tags.size > 1, "Must have at least two tags")
    override val sortKey = Seq(3, Padding, tags.map(_.sortKey).mkString("|")).mkString(Divider)
    override val major = tags.head.major
    override def nextMicro() = None
  }

  case class Unknown(version: String) extends VersionTag {
    override val sortKey: String = Seq(1, Padding, version.toLowerCase).mkString(Divider)
    override val major = None
    override def nextMicro() = None
  }

}

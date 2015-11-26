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

  private[lib] val SemverRx = """^([a-z]?)([\d\.]+)$""".r
  private[lib] val SemverWithTextRx = """^([a-z]?)([\d\.]+)[\.\_\-]([\w]+)$""".r

  def apply(value: String): VersionTag = {
    value.trim.split(VersionTag.Dash).flatMap(fromString(_)).toList match {
      case Nil => Unknown(value)
      case one :: Nil => one
      case multiple => Multi(value, multiple)
    }
  }

  private def fromString(value: String): Option[VersionTag] = {
    value.trim match {
      case SemverRx(leadingText, versions) => {
        toSemverOrDate(versions, leadingText)
      }
      case SemverWithTextRx(leadingText, versions, text) => {
        toSemverOrDate(versions, leadingText) match {
          case None => {
            Some(Unknown(value))
          }
          case Some(semver) => {
            Some(
              Multi(
                value,
                Seq(
                  semver,
                  Unknown(text)
                )
              )
            )
          }
        }
      }
      case _ => {
        Some(Unknown(value.trim))
      }
    }
  }

  private[this] def toSemverOrDate(value: String, leadingText: String): Option[VersionTag] = {
    val fullVersion = s"${leadingText}$value"
    value.split(VersionTag.Dot).map(_.toInt).toList match {
      case Nil => None
      case major :: Nil => Some(Semver(fullVersion, major, 0, 0))
      case major :: minor :: Nil => {
        isDate(major) match {
          case true => Some(Date(fullVersion, major, minor))
          case false => Some(Semver(fullVersion, major, minor, 0))
        }
      }
      case major :: minor :: micro :: rest => {
        Some(Semver(fullVersion, major, minor, micro))
      }
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

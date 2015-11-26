package com.bryzek.dependency.lib

import scala.util.parsing.combinator._

case class Version(value: String, tags: Seq[Tag]) extends Ordered[Version] {

  /**
    * If present, the major version number
    */
  val major: Option[Int] = {
    tags.headOption.flatMap { tag =>
      tag match {
        case Tag.Semver(major, _, _, _) => Some(major)
        case Tag.Date(_, _) | Tag.Text(_) => None
      }
    }
  }

  /**
    * If possible, we construct the next micro version number higher
    * than the present version. General algorithm is to find the first
    * semver or date tag and then increment its smallest version
    * (micro for semver, minor for date).
    */
  def nextMicro(): Option[Version] = {
    var found: Option[(Tag, Tag)] = None
    val newTags = tags.map { tag =>
      (found, tag) match {
        case (Some(_), _) => tag
        case (None, t: Tag.Text) => t
        case (None, t: Tag.Semver) => {
          found = Some(t -> t.nextMicro)
          t.nextMicro
        }
        case (None, t: Tag.Date) => {
          found = Some(t -> t.nextMicro)
          t.nextMicro
        }
      }
    }
    found.map { case (oldTag, newTag) =>
      Version(value.replace(oldTag.value, newTag.value), newTags)
    }
  }

  /**
    * Note that we want to make sure that the simple semver versions
    * sort highest - thus if we have exactly one tag that is semver,
    * bump up its priority. This allows for 1.0.0 to sort
    * about 1.0.0-dev (as one example).
    */
  val sortKey: String = {
    tags match {
      case one :: Nil => {
        one match {
          case tag: Tag.Semver => tag.sortKeyWithPrefix(80)
          case _ => one.sortKey
        }
      }
      case multiple => multiple.map(_.sortKey).mkString(",")
    }
  }

  def compare(that: Version) = {
    sortKey.compare(that.sortKey)
  }

}

sealed trait Tag extends Ordered[Tag] {

  /**
    * Constucts string repesentation of this tag
    */
  val value: String

  /**
    * Constructs a lexicographic sort key for this tag
    */
  val sortKey: String

  def compare(that: Tag) = {
    sortKey.compare(that.sortKey)
  }

}

object Tag {

  private[this] val Padding = 10000

  // Any text in the tag (e.g. final)
  case class Text(value: String) extends Tag {
    override val sortKey: String = Seq(20, value.trim.toLowerCase).mkString(".")
  }

  // Tags that look like r20151211.1
  case class Date(date: Long, minorNum: Int) extends Tag {
    assert(VersionParser.isDate(date), s"Must be a date[$date]")
    override val value: String = s"${date}.$minorNum"
    override val sortKey: String = Seq(40, date, minorNum + Padding).mkString(".")
    def nextMicro: Tag = Date(date, minorNum + 1)
  }

  // Tags that look like 1.2.3 (semantic versioning... preferred)
  case class Semver(major: Int, minor: Int, micro: Int, additional: Seq[Int] = Nil) extends Tag {
    private[this] val all = (Seq(major, minor, micro) ++ additional)

    override val value: String = all.mkString(".")
    override val sortKey: String = sortKeyWithPrefix(60)

    def sortKeyWithPrefix(prefix: Int) = Seq(
      prefix,
      all.map(_ + Padding).mkString(".")
    ).mkString(".")

    def nextMicro: Semver = additional match {
      case Nil => Semver(major, minor, micro + 1)
      case some => {
        Semver(major, minor, micro, additional.dropRight(1) ++ Seq(some.last + 1))
      }
    }
  }

}

object VersionParser {
  def parse(input: String): Version = {
    input.trim match {
      case "" => {
        Version(input, Nil)
      }
      case value => {
        val parser = new VersionParser()
        val tags: Seq[Tag] = parser.parseAll(parser.tag, value) match {
          case parser.Success(result, _) => {
            // If we have multiple tags, and the first tag is a one
            // character Text tag, we strip the leading tag. This
            // provides support for release numbers as in git hub -
            // e.g. "r1.2.3" - converting that to a simple semver
            // "1.2.3"
            result match {
              case Tag.Text(value) :: rest => {
                (value.size == 1) match {
                  case true => rest
                  case false => result
                }
              }
              case _ => result
            }
          }
          case parser.Error(msg, _) => sys.error(s"error while parsing version[$value]: $msg")
          case parser.Failure(msg, _) => sys.error(s"failure while parsing version[$value]: $msg")
        }
        Version(input, tags)
      }
    }
  }

  private[lib] def isDate(value: Long): Boolean = {
    value.toString.length >= 8 && value.toString.substring(0, 4).toInt >= 1900
  }

}

class VersionParser extends RegexParsers {
  def number: Parser[Int] = """[0-9]+""".r ^^ { _.toInt }
  def text: Parser[Tag.Text] = """[a-zA-Z]+""".r ^^ { case value => Tag.Text(value.toString) }
  def dot: Parser[String] = """\.""".r ^^ { _.toString }
  def divider: Parser[String] = """[\.\_\-]+""".r ^^ { _.toString }
  def semver: Parser[Tag] = rep1sep(number, dot) ^^ {
    case Nil => {
      Tag.Semver(0, 0, 0)
    }
    case major :: Nil => {
      VersionParser.isDate(major) match {
        case true => Tag.Date(major, 0)
        case false => Tag.Semver(major, 0, 0)
      }
    }
    case major :: minor :: Nil => {
      VersionParser.isDate(major) match {
        case true => Tag.Date(major, minor)
        case false => Tag.Semver(major, minor, 0)
      }
    }
    case major :: minor :: micro :: additional => {
      Tag.Semver(major, minor, micro, additional)
    }
  }
  def tag: Parser[List[Tag]] = rep1sep(semver | text, opt(divider))
}

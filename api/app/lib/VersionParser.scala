package com.bryzek.dependency.lib

import scala.util.parsing.combinator._

case class Version(value: String, tags: Seq[Tag])

sealed trait Tag
object Tag {

  case class Text(value: String) extends Tag
  case class Semver(major: Int, minor: Int, micro: Int, additional: Seq[Int] = Nil) extends Tag

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
          case parser.Success(result, _) => result
          case parser.Error(msg, _) => sys.error(s"error while parsing version[$value]: $msg")
          case parser.Failure(msg, _) => sys.error(s"failure while parsing version[$value]: $msg")
        }
        Version(input, tags)
      }
    }
  }
  
}

class VersionParser extends RegexParsers {
  def number: Parser[Int] = """[0-9]+""".r ^^ { _.toInt }
  def text: Parser[Tag.Text] = """[a-zA-Z]+""".r ^^ { case value => Tag.Text(value.toString) }
  def dot: Parser[String] = """\.""".r ^^ { _.toString }
  def divider: Parser[String] = """[\.\_\-]+""".r ^^ { _.toString }
  def semver: Parser[Tag.Semver] = rep1sep(number, dot) ^^ {
    case Nil => {
      Tag.Semver(0, 0, 0)
    }
    case major :: Nil => {
      Tag.Semver(major, 0, 0)
    }
    case major :: minor :: Nil => {
      Tag.Semver(major, minor, 0)
    }
    case major :: minor :: micro :: additional => {
      Tag.Semver(major, minor, micro, additional)
    }
  }
  def tag: Parser[List[Tag]] = rep1sep(semver | text, opt(divider))
}

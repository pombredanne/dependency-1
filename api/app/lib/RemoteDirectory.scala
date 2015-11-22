package com.bryzek.dependency.lib

import org.htmlcleaner.HtmlCleaner
import org.apache.commons.lang3.{StringEscapeUtils, StringUtils}
import java.net.URL
import scala.util.{Failure, Success, Try}
import play.api.Logger

/**
  * Accepts the URI of a resolver
  * (e.g. https://oss.sonatype.org/content/repositories/snapshots) and
  * parsers the contents in a list of files and
  * directories. Intentionally NOT recursive.
  */
object RemoteDirectory {

  case class Result(
    directories: Seq[String] = Nil,
    files: Seq[String] = Nil
  )

  def fetch(
    url: String
  ) (
    filter: String => Boolean = { !_.startsWith(".") }
  ): Result = {
    val base = Result()
    val cleaner = new HtmlCleaner()

    Try(cleaner.clean(new URL(url))) match {
      case Failure(ex) => ex match {
        case e: java.io.FileNotFoundException => {
          base
        }
        case _ => {
          Logger.error("Error fetching URL[$url]: $ex")
          base
        }
      }
      case Success(rootNode) => {
        rootNode.getElementsByName("a", true).foldLeft(base) { case (result, elem) =>
          Option(elem.getAttributeByName("href")) match {
            case None => {
              result
            }
            case Some(rawHref) => {
              val text = StringEscapeUtils.unescapeHtml4(elem.getText.toString)
              filter(StringUtils.stripEnd(text, "/")) match {
                case false => {
                  result
                }
                case true => {
                  text.endsWith("/") match {
                    case true => result.copy(directories = result.directories ++ Seq(text))
                    case false => result.copy(files = result.files ++ Seq(text))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

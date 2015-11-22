package com.bryzek.dependency.lib

import org.htmlcleaner.HtmlCleaner
import org.apache.commons.lang3.{StringEscapeUtils, StringUtils}
import java.net.URL
import scala.util.{Failure, Success, Try}
import play.api.Logger

object RemoteDirectory {

  case class Result(
    url: String,
    files: Seq[String] = Nil,
    directories: Seq[String] = Nil
  )

  def fetch(
    resolver: String,
    groupId: String,
    artifactId: String
  ) (
    filter: String => Boolean = { name => name == artifactId || name.startsWith(artifactId + "_") }
  ): Result = {
    val base = Result(url = makeUrl(resolver, groupId))
    println(s"==> Fetching ${base.url}")

    val cleaner = new HtmlCleaner()
    Try(cleaner.clean(new URL(base.url))) match {
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
            case None => result
            case Some(rawHref) => {
              val text = StringEscapeUtils.unescapeHtml4(elem.getText.toString)
              val href =StringEscapeUtils.unescapeHtml4(rawHref)
              println(s" - text[$text] href[$href]")
              filter(text) match {
                case false => result
                case true => {
                  text.endsWith("/") match {
                    case true => result.copy(directories = result.directories ++ Seq(StringUtils.stripEnd(text, "/") ))
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

  def makeUrl(
    resolver: String,
    groupId: String
  ): String = {
    Seq(
      resolver,
      groupId.replaceAll("\\.", "/")
    ).map ( StringUtils.stripEnd(_, "/") ).mkString("/")
  }
}

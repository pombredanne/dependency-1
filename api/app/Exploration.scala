import org.htmlcleaner.HtmlCleaner
import org.apache.commons.lang3.StringEscapeUtils
import java.net.URL

object Exploration {

  case class UrlResult(
    url: String,
    files: Seq[String] = Nil,
    directories: Seq[String] = Nil
  )

  def test() {
    println("")
    getHeadlinesFromUrl("http://dl.bintray.com/typesafe/maven-releases/com/typesafe/play/play_2.11/")
  }

  def getHeadlinesFromUrl(url: String): UrlResult = {
    println(s"==> Fetching $url")
    val cleaner = new HtmlCleaner()
    val rootNode = cleaner.clean(new URL(url))
    var result = UrlResult(url = url)
    rootNode.getElementsByName("a", true).foreach { elem =>
      Option(elem.getAttributeByName("href")).foreach { rawHref =>
        val text = StringEscapeUtils.unescapeHtml4(elem.getText.toString)
        val href =StringEscapeUtils.unescapeHtml4(rawHref)
        println(s" - text[$text] href[$href]")
        text.endsWith("/") match {
          case true => result = result.copy(directories = result.directories ++ Seq(text))
          case false => result = result.copy(files = result.files ++ Seq(text))
        }
      }
    }
    result
  }

}

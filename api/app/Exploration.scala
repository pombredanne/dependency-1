import org.htmlcleaner.HtmlCleaner
import org.apache.commons.lang3.StringEscapeUtils
import java.net.URL

object Exploration {

  def test() {
    // getHeadlinesFromUrl("http://dl.bintray.com/typesafe/maven-releases/com/typesafe/play/play_2.11/")
    getHeadlinesFromUrl("file:///web/dependency/play_2.11.html")
  }

  def getHeadlinesFromUrl(url: String): Seq[String] = {
    val cleaner = new HtmlCleaner()
    val rootNode = cleaner.clean(new URL(url))
    rootNode.getElementsByName("a", true).flatMap { elem =>
      Option(elem.getAttributeByName("href")).flatMap { rawHref =>
        val text = StringEscapeUtils.unescapeHtml4(elem.getText.toString)
        val href =StringEscapeUtils.unescapeHtml4(rawHref)
        println(s" - text[$text] href[$href]")
        Some(href)
      }
    }.toSeq
  }

}

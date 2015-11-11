import org.htmlcleaner.HtmlCleaner
import org.apache.commons.lang3.StringEscapeUtils
import java.net.URL

object Exploration {

  def test() {
    getHeadlinesFromUrl("http://dl.bintray.com/typesafe/maven-releases/com/typesafe/play/play_2.11/")
  }

  def getHeadlinesFromUrl(url: String): Iterable[String] = {
    val cleaner = new HtmlCleaner()
    // val props = cleaner.getProperties
    val rootNode = cleaner.clean(new URL(url))
    rootNode.getElementsByName("a", true).flatMap { elem =>
      println("ELEMENT: "+ elem)
      Option(elem.getAttributeByName("class")) match {
        case None => {
          // ignore
          None
        }
        case Some(className) => {
          // stories might be "dirty" with text like "'", clean it up
          Some(StringEscapeUtils.unescapeHtml4(elem.getText.toString))
        }
      }
    }.toSeq
  }

}

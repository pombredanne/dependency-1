import io.flow.maven.v0.Client

import play.api._
import play.api.mvc._
import play.api.libs.json._

object Maven {

  import scala.concurrent.ExecutionContext.Implicits.global

  val MavenUrl = "http://search.maven.org/solrsearch"

  lazy val client = new Client(MavenUrl)

  def debug(value: String) {
    println("")
    println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
    println(value)
    println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
    println("")
  }

  def test() {
    debug("starting")

    client.docs.getSelect("g:com.google.inject AND a:guice").map { apiResponse =>
      println("got response")
      println(" - numFound: " + apiResponse.response.numFound)
      println(" - start: " + apiResponse.response.start)
      println( " - documents:")
      apiResponse.response.docs.foreach { doc =>
        println( "  - doc: " + doc)
      }
    }

    Thread.sleep(1000)
  }

}

import io.flow.maven.v0.Client

import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.Future

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
    latestVersion("com.google.inject", "guice").map { version =>
      version match {
        case None => debug("no version found")
        case v => debug(s"Version: $v")
      }
    }
  }

  def latestVersion(
    groupId: String,
    artifactId: String
  ): Future[Option[String]] = {
    client.docs.getSelect(s"g:$groupId AND a:$artifactId").map { apiResponse =>
      apiResponse.response.docs.map(_.latestVersion) match {
        case Nil => None
        case version :: Nil => Some(version)
        case multiple => {
          play.api.Logger.warn(s"Multiple versions found for group[$groupId] artifact[$artifactId]")
          multiple.headOption
        }
      }
    }
  }
}

import io.flow.github.v0.Client
import io.flow.github.v0.errors.UnitResponse
import io.flow.github.v0.models.{Contents, Encoding}
import org.apache.commons.codec.binary.Base64

import play.api._
import play.api.mvc._
import play.api.libs.json._

object Exploration {

  import scala.concurrent.ExecutionContext.Implicits.global

  val client = new Client(
    apiUrl = "https://api.github.com",
    defaultHeaders = Seq(
      "Authorization" -> "token fcb5ad1715906c962d5b96d885c0c67d7c3cb683"
    )
  )

  def debug(value: String) {
    println("")
    println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
    println(value)
    println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
    println("")
  }

  def test() {
    debug("starting")
    // client.repositories.getUserAndRepos(
    //   affiliation = Some("owner")
    // ).map { repositories =>

    client.repositories.getOrgsAndReposByOrg(
      org = "flowcommerce"
    ).map { repositories =>
      repositories.foreach { repo =>
        debug(s" - repo owner[${repo.owner.login}] name[${repo.name}]")
        client.contents.getReposByOwnerAndRepoAndPath(
          owner = repo.owner.login,
          repo = repo.name,
          path = "build.sbt"
        ).map { contents =>
          debug(" - got file: " + toText(contents))
        }.recover {
          case UnitResponse(404) => debug(" - file not found")
        }
      }
    }
  }

  def toText(contents: Contents): String = {
    (contents.content, contents.encoding) match {
      case (Some(encoded), Encoding.Base64) => {
        new String(Base64.decodeBase64(encoded.getBytes))
      }
      case (Some(_), Encoding.UNDEFINED(name)) => {
        sys.error(s"Unsupported encoding[$name] for content: $contents")
      }
      case (None, _) => {
        sys.error(s"No contents for: $contents")
      }
    }
  }

}

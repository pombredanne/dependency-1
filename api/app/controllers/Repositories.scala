package controllers

import com.bryzek.dependency.lib.Github
import com.bryzek.dependency.v0.models.GithubAuthenticationForm
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import io.flow.user.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.Future

class Repositories @javax.inject.Inject() (
  val userTokensClient: UserTokensClient,
  val github: Github
) extends Controller with IdentifiedRestController {

  import scala.concurrent.ExecutionContext.Implicits.global

  def getGithub(
    limit: Long = 25,
    offset: Long = 0
  ) = Identified.async(parse.json) { request =>
    for {
      repos <- github.repositories(request.user)
    } yield {
      Ok(Json.toJson(repos))
    }
  }

}


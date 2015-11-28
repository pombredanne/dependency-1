package controllers

import db.{UsersDao, GithubUsersDao}
import io.flow.common.v0.models.Error
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.AnonymousRestController
import io.flow.play.util.Validation
import io.flow.user.v0.models.{User, UserForm}
import io.flow.user.v0.models.json._
import com.bryzek.dependency.v0.models.{GithubAuthenticationForm, GithubUserForm}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._

class GithubUsers @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with AnonymousRestController {

  import scala.concurrent.ExecutionContext.Implicits.global

  def postAuthenticationsAndGithub() = Action(parse.json) { request =>
    request.body.validate[GithubAuthenticationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[GithubAuthenticationForm] => {
        val form = s.get
        sys.error("TODO")
      }
    }
  }

}

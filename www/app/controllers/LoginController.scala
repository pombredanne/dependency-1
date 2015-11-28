package controllers

import com.bryzek.dependency.lib.{DependencyClientProvider, UiData}
import com.bryzek.dependency.v0.models.GithubAuthenticationForm
import play.api._
import play.api.i18n._
import play.api.mvc._

class LoginController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  val provider: DependencyClientProvider
) extends Controller
    with I18nSupport
{

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(returnUrl: Option[String]) = Action { implicit request =>
    Ok(views.html.login.index(UiData(requestPath = request.path), returnUrl))
  }

  def githubCallback(
    code: String,
    state: Option[String]
  ) = Action.async { implicit request =>
    val returnUrl = None // TODO
    provider.newClient(None).githubUsers.postAuthenticationsAndGithub(
      GithubAuthenticationForm(
        code = code
      )
    ).map { user =>
      Redirect(routes.ApplicationController.index()).withSession { "user_guid" -> user.guid.toString }
    }.recover {
      case response: com.bryzek.dependency.v0.errors.ErrorsResponse => {
        Ok(views.html.login.index(UiData(requestPath = request.path), returnUrl, response.errors.map(_.message)))
      }
    }
  }

}

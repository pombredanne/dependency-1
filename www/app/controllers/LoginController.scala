package controllers

import com.bryzek.dependency.v0.errors.ErrorsResponse
import com.bryzek.dependency.v0.models.{AuthenticationForm, UserForm}
import com.bryzek.dependency.lib.{DependencyClientProvider, UiData}
import io.flow.play.util.Validation
import play.api._
import play.api.i18n._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class LoginController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  val provider: DependencyClientProvider
) extends Controller with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val client = provider.newClient(user = None)

  def redirect = Action {
    Redirect(routes.LoginController.index())
  }

  def index(returnUrl: Option[String]) = Action { implicit request =>
    val form = LoginController.loginForm.fill(LoginController.LoginData(email = "", returnUrl = returnUrl))
    Ok(views.html.login.index(UiData(requestPath = request.path), form))
  }

  def indexPost() = Action.async { implicit request =>
    val form = LoginController.loginForm.bindFromRequest
    form.fold (

      formWithErrors => Future {
        Ok(views.html.login.index(UiData(requestPath = request.path), formWithErrors))
      },

      validForm => {
        val returnUrl = validForm.returnUrl.getOrElse("/")

        client.users.postAuthenticate(AuthenticationForm(email = validForm.email.trim)).map { user =>
          Redirect(returnUrl).withSession { "user_guid" -> user.guid.toString }
        }.recover {
          case r: ErrorsResponse => {
            r.errors.map(_.code).toList match {
              case Validation.Codes.UserAuthorizationFailed :: Nil => {
                // For now, just auto-register the user if the email is valid
                try {
                  val user = Await.result(
                    client.users.post(UserForm(email = validForm.email.trim)),
                    1000.millis
                  )
                  Redirect(returnUrl).withSession { "user_guid" -> user.guid.toString }
                } catch {
                  case r: ErrorsResponse => {
                    Ok(views.html.login.index(UiData(requestPath = request.path), form, r.errors.map(_.message)))
                  }
                }
              }
              case _ => {
                Ok(views.html.login.index(UiData(requestPath = request.path), form, r.errors.map(_.message)))
              }
            }
          }
        }
      }
    )
  }
}

object LoginController {

  case class LoginData(
    email: String,
    returnUrl: Option[String]
  )

  val loginForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "return_url" -> optional(text)
    )(LoginData.apply)(LoginData.unapply)
  )

}

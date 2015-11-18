package controllers

import com.bryzek.dependency.lib.{DependencyClientProvider, UiData}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import play.api._
import play.api.i18n._
import play.api.mvc._

class ApplicationController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  val userTokensClient: UserTokensClient,
  val dependencyClientProvider: DependencyClientProvider
) extends Controller with IdentifiedRestController with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def unauthorized[A](request: Request[A]): Result = {
    Redirect(routes.LoginController.index(return_url = Some(request.path))).flashing("warning" -> "Please login")
  }

  def index() = Identified { implicit request =>
    Ok(
      views.html.index(
        UiData(requestPath = request.path, user = Some(request.user))
      )
    )
  }

}

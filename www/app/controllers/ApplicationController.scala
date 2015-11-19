package controllers

import com.bryzek.dependency.lib.{DependencyClientProvider, UiData}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedController
import play.api._
import play.api.i18n._
import play.api.mvc._

class ApplicationController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  val userTokensClient: UserTokensClient,
  val dependencyClientProvider: DependencyClientProvider
) extends Controller
    with IdentifiedController
    with I18nSupport
{

  import scala.concurrent.ExecutionContext.Implicits.global

  override def unauthorized[A](request: Request[A]): Result = {
    Redirect(routes.LoginController.index(return_url = Some(request.path))).flashing("warning" -> "Please login")
  }

  override def user(
    session: play.api.mvc.Session,
    headers: play.api.mvc.Headers
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): scala.concurrent.Future[Option[io.flow.user.v0.models.User]] = {
    session.get("user_guid") match {
      case None => scala.concurrent.Future { None }
      case Some(userGuid) => userTokensClient.getUserByToken(userGuid)
    }
  }

  def index() = Identified { implicit request =>
    Ok(
      views.html.index(
        UiData(requestPath = request.path, user = Some(request.user))
      )
    )
  }

}

package controllers

import com.bryzek.dependency.lib.DependencyClientProvider
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedController
import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._

class ApplicationController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient) {

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
        uiData(request)
      )
    )
  }

}

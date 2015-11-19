package controllers

import com.bryzek.dependency.lib.UiData
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedController
import play.api._
import play.api.i18n._
import play.api.mvc._

abstract class BaseController(
  val userTokensClient: UserTokensClient
) extends Controller
    with IdentifiedController
    with I18nSupport
{

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

  def uiData[T](request: IdentifiedRequest[T]): UiData = {
    UiData(requestPath = request.path, user = Some(request.user))
  }

}

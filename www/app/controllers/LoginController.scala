package controllers

import play.api.data._
import play.api.data.Forms._

import play.api._
import play.api.i18n._
import play.api.mvc.{Action, Controller}

class LoginController @javax.inject.Inject() (
  val messagesApi: MessagesApi
) extends Controller with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action {
    Redirect(routes.LoginController.index())
  }

  def index(returnUrl: Option[String]) = TODO

  def indexPost = TODO

}

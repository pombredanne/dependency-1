package controllers

import lib.UiData
import play.api._
import play.api.i18n._
import play.api.mvc._

class ApplicationController @javax.inject.Inject() (
  val messagesApi: MessagesApi
) extends Controller with I18nSupport {

  def index() = Action { implicit request =>
    Ok(
      views.html.index(
        UiData(requestPath = request.path)
      )
    )
  }

}

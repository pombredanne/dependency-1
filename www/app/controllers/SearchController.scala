package controllers

import play.api._
import play.api.i18n._
import play.api.mvc._

class SearchController @javax.inject.Inject() (
  val messagesApi: MessagesApi
) extends Controller with I18nSupport {

  def index(
    q: Option[String],
    page: Int
  ) = TODO

}

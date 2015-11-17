package controllers

import com.bryzek.dependency.lib.{DependencyClientProvider, UiData}
import play.api._
import play.api.i18n._
import play.api.mvc._

class ApplicationController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  implicit val dependencyClientProvider: DependencyClientProvider
) extends Controller with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index() = Authenticated() { implicit request =>
    Ok(
      views.html.index(
        request.uiData()
      )
    )
  }

}

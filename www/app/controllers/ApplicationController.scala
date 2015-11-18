package controllers

import com.bryzek.dependency.lib.{DependencyClientProvider, UiData}
import io.flow.play.clients.UserTokenClient
import io.flow.play.controllers.IdentifiedRestController
import play.api._
import play.api.i18n._
import play.api.mvc._

class ApplicationController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  val userTokensClient: UserTokenClient,
  val dependencyClientProvider: DependencyClientProvider
) extends Controller with IdentifiedRestController with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index() = Identified { implicit request =>
    Ok(
      views.html.index(
        UiData(requestPath = request.path, user = Some(request.user))
      )
    )
  }

}

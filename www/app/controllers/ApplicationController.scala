package controllers

import com.bryzek.dependency.lib.DependencyClientProvider
import io.flow.play.clients.UserTokensClient
import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._

class ApplicationController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global
 
  def redirect = Action { request =>
    Redirect(request.path + "/")
  }

  def index() = Identified { implicit request =>
    Ok(
      views.html.index(
        uiData(request)
      )
    )
  }

}

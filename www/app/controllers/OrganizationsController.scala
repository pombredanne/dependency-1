package controllers

import com.bryzek.dependency.www.lib.DependencyClientProvider
import io.flow.play.clients.UserTokensClient
import io.flow.play.util.{Pagination, PaginatedCollection}
import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._

class OrganizationsController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global
 
  override def section = Some(com.bryzek.dependency.www.lib.Section.Dashboard)

  def index() = Identified { implicit request =>
    sys.error("TODO")
  }

  def show(org: String) = Identified { implicit request =>
    Redirect(routes.ApplicationController.index(organization = Some(org)))
  }

}

package controllers

import com.bryzek.dependency.lib.{DependencyClientProvider, QueryParser}
import io.flow.play.clients.UserTokensClient
import io.flow.play.util.{Pagination, PaginatedCollection}

import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._

class SearchController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  def index(
    q: Option[String],
    page: Int
  ) = Anonymous.async { implicit request =>
    val clause: Option[QueryParser.Clause] = q.map(_.trim).getOrElse("") match {
      case "" => None
      case value => QueryParser.parse(value) match {
        case Left(errors) => sys.error(errors.mkString(","))
        case Right(result) => Some(result)
      }
    }
    sys.error("TODO: " + clause)
  }

}

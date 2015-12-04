package controllers

import com.bryzek.dependency.lib.{DependencyClientProvider, SearchQuery}
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

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(
    q: Option[String],
    page: Int
  ) = Identified.async { implicit request =>
    val query = SearchQuery.parse(q.getOrElse(""))
    println("QUERY: " + query)

    for {
      items <- dependencyClient(request).items.getSearch(
        q = q,
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(
        views.html.search.index(
          uiData(request),
          q,
          PaginatedCollection(page, items)
        )
      )
    }
  }

}

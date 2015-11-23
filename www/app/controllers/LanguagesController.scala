package controllers

import com.bryzek.dependency.v0.errors.UnitResponse
import com.bryzek.dependency.v0.models.Language
import com.bryzek.dependency.lib.DependencyClientProvider
import io.flow.play.clients.UserTokensClient
import io.flow.play.util.{Pagination, PaginatedCollection}
import java.util.UUID
import scala.concurrent.Future

import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._

class LanguagesController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(page: Int = 0) = Identified.async { implicit request =>
    for {
      languages <- dependencyClient(request).languages.get(
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(
        views.html.languages.index(
          uiData(request),
          PaginatedCollection(page, languages)
        )
      )
    }
  }

  def show(
    guid: UUID,
    projectsPage: Int = 0
  ) = Identified.async { implicit request =>
    withLanguage(request, guid) { language =>
      for {
        versions <- dependencyClient(request).languageVersions.get(
          languageGuid = Some(guid),
          limit = 5+1,
          offset = 0
        )
        projectLanguageVersions <- dependencyClient(request).projectLanguageVersions.get(
          languageGuid = Some(guid),
          limit = Pagination.DefaultLimit+1,
          offset = projectsPage * Pagination.DefaultLimit
        )
      } yield {
        Ok(
          views.html.languages.show(
            uiData(request),
            language,
            PaginatedCollection(0, versions, 5),
            PaginatedCollection(projectsPage, projectLanguageVersions)
          )
        )
      }
    }
  }

  def withLanguage[T](
    request: IdentifiedRequest[T],
    guid: UUID
  )(
    f: Language => Future[Result]
  ) = {
    dependencyClient(request).languages.getByGuid(guid).flatMap { language =>
      f(language)
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.LanguagesController.index()).flashing("warning" -> s"Language not found")
      }
    }
  }

}

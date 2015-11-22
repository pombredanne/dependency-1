package controllers

import com.bryzek.dependency.v0.errors.UnitResponse
import com.bryzek.dependency.v0.models.{Library, LibraryForm, Scms}
import com.bryzek.dependency.lib.DependencyClientProvider
import io.flow.play.clients.UserTokensClient
import io.flow.play.util.{Pagination, PaginatedCollection}
import java.util.UUID
import scala.concurrent.Future

import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

class LibrariesController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(page: Int = 0) = Identified.async { implicit request =>
    for {
      libraries <- dependencyClient(request).libraries.get(
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(
        views.html.libraries.index(
          uiData(request),
          PaginatedCollection(page, libraries)
        )
      )
    }
  }

  def show(
    guid: UUID,
    versionsPage: Int = 0,
    projectsPage: Int = 0
  ) = Identified.async { implicit request =>
    withLibrary(request, guid) { library =>
      for {
        versions <- dependencyClient(request).libraryVersions.get(libraryGuid = Some(guid))
        projectLibraryVersions <- dependencyClient(request).projectLibraryVersions.get(libraryGuid = Some(guid))
      } yield {
        Ok(
          views.html.libraries.show(
            uiData(request),
            library,
            PaginatedCollection(versionsPage, versions),
            PaginatedCollection(projectsPage, projectLibraryVersions)
          )
        )
      }
    }
  }

  def withLibrary[T](
    request: IdentifiedRequest[T],
    guid: UUID
  )(
    f: Library => Future[Result]
  ) = {
    dependencyClient(request).libraries.getByGuid(guid).flatMap { library =>
      f(library)
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.LibrariesController.index()).flashing("warning" -> s"Library not found")
      }
    }
  }

}

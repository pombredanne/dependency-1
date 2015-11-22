package controllers

import com.bryzek.dependency.v0.errors.UnitResponse
import com.bryzek.dependency.v0.models.LibraryVersion
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

class LibraryVersionsController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(page: Int = 0) = Identified {
    Redirect(routes.LibrariesController.index())
  }

  def show(
    guid: UUID,
    versionsPage: Int = 0,
    projectsPage: Int = 0
  ) = Identified.async { implicit request =>
    withLibraryVersion(request, guid) { libraryVersion =>
      for {
        projects <- dependencyClient(request).projects.get(libraryVersionGuid = Some(guid))
      } yield {
        Ok(
          views.html.libraries.versions.show(
            uiData(request),
            libraryVersion,
            PaginatedCollection(projectsPage, projects)
          )
        )
      }
    }
  }

  def withLibraryVersion[T](
    request: IdentifiedRequest[T],
    guid: UUID
  )(
    f: LibraryVersion => Future[Result]
  ) = {
    println(s"GUID[$guid]")
    dependencyClient(request).libraryVersions.getByGuid(guid).flatMap { libraryVersion =>
      f(libraryVersion)
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.LibrariesController.index()).flashing("warning" -> s"Library version not found")
      }
    }
  }

}

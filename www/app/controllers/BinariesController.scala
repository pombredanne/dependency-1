package controllers

import com.bryzek.dependency.v0.errors.UnitResponse
import com.bryzek.dependency.v0.models.Binary
import com.bryzek.dependency.www.lib.DependencyClientProvider
import io.flow.play.clients.UserTokensClient
import io.flow.play.util.{Pagination, PaginatedCollection}
import java.util.UUID
import scala.concurrent.Future

import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._

class BinariesController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def section = Some(com.bryzek.dependency.www.lib.Section.Binaries)

  def index(page: Int = 0) = Identified.async { implicit request =>
    for {
      binaries <- dependencyClient(request).binaries.get(
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(
        views.html.binaries.index(
          uiData(request),
          PaginatedCollection(page, binaries)
        )
      )
    }
  }

  def show(
    guid: UUID,
    projectsPage: Int = 0
  ) = Identified.async { implicit request =>
    withBinary(request, guid) { binary =>
      for {
        versions <- dependencyClient(request).binaryVersions.get(
          binaryGuid = Some(guid),
          limit = 5+1,
          offset = 0
        )
        projectBinaryVersions <- dependencyClient(request).projectBinaryVersions.get(
          binaryGuid = Some(guid),
          limit = Pagination.DefaultLimit+1,
          offset = projectsPage * Pagination.DefaultLimit
        )
      } yield {
        Ok(
          views.html.binaries.show(
            uiData(request),
            binary,
            PaginatedCollection(0, versions, 5),
            PaginatedCollection(projectsPage, projectBinaryVersions)
          )
        )
      }
    }
  }

  def withBinary[T](
    request: IdentifiedRequest[T],
    guid: UUID
  )(
    f: Binary => Future[Result]
  ) = {
    dependencyClient(request).binaries.getByGuid(guid).flatMap { binary =>
      f(binary)
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.BinariesController.index()).flashing("warning" -> s"Binary not found")
      }
    }
  }

}

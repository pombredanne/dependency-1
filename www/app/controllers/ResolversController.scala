package controllers

import com.bryzek.dependency.v0.errors.UnitResponse
import com.bryzek.dependency.v0.models.{Resolver, ResolverForm, UsernamePassword, Visibility}
import com.bryzek.dependency.lib.DependencyClientProvider
import io.flow.user.v0.models.User
import io.flow.play.clients.UserTokensClient
import io.flow.play.util.{Pagination, PaginatedCollection}
import java.util.UUID
import scala.concurrent.Future

import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

class ResolversController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def section = Some(com.bryzek.dependency.lib.Section.Resolvers)

  def index(page: Int = 0) = Identified.async { implicit request =>
    for {
      resolvers <- dependencyClient(request).resolvers.get(
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(
        views.html.resolvers.index(
          uiData(request),
          PaginatedCollection(page, resolvers)
        )
      )
    }
  }

  def show(guid: UUID, binariesPage: Int = 0, librariesPage: Int = 0) = Identified.async { implicit request =>
    withResolver(request, guid) { resolver =>
      Future {
        Ok(
          views.html.resolvers.show(
            uiData(request),
            resolver
          )
        )
      }
    }
  }

  def create() = Identified { implicit request =>
    Ok(
      views.html.resolvers.create(
        uiData(request), ResolversController.uiForm
      )
    )
  }

  def postCreate() = Identified.async { implicit request =>
    val boundForm = ResolversController.uiForm.bindFromRequest
    boundForm.fold (

      formWithErrors => Future {
        Ok(views.html.resolvers.create(uiData(request), formWithErrors))
      },

      uiForm => {
        dependencyClient(request).resolvers.post(
          resolverForm = uiForm.resolverForm(request.user)
        ).map { resolver =>
          Redirect(routes.ResolversController.show(resolver.guid)).flashing("success" -> "Resolver created")
        }.recover {
          case response: com.bryzek.dependency.v0.errors.ErrorsResponse => {
            Ok(views.html.resolvers.create(uiData(request), boundForm, response.errors.map(_.message)))
          }
        }
      }
    )
  }

  def withResolver[T](
    request: IdentifiedRequest[T],
    guid: UUID
  )(
    f: Resolver => Future[Result]
  ) = {
    dependencyClient(request).resolvers.getByGuid(guid).flatMap { resolver =>
      f(resolver)
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.ResolversController.index()).flashing("warning" -> s"Resolver not found")
      }
    }
  }

  def postDelete(guid: UUID) = Identified.async { implicit request =>
    dependencyClient(request).resolvers.deleteByGuid(guid).map { response =>
      Redirect(routes.ResolversController.index()).flashing("success" -> s"Resolver deleted")
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.ResolversController.index()).flashing("warning" -> s"Resolver not found")
      }
    }
  }

}

object ResolversController {

  case class UiForm(
    uri: String,
    username: Option[String],
    password: Option[String]
  ) {

    def resolverForm(user: User) = ResolverForm(
      visibility = Visibility.Private,
      userGuid = user.guid,
      uri = uri,
      credentials = credentials
    )

    val credentials = username.map(_.trim) match {
      case None => None
      case Some("") => None
      case Some(username) => {
        Some(
          UsernamePassword(
            username = username,
            password = password.map(_.trim)
          )
        )
      }
    }

  }

  private val uiForm = Form(
    mapping(
      "uri" -> nonEmptyText,
      "username" -> optional(text),
      "password" -> optional(text)
    )(UiForm.apply)(UiForm.unapply)
  )

}

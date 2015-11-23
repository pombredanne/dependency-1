package controllers

import com.bryzek.dependency.v0.errors.UnitResponse
import com.bryzek.dependency.v0.models.{Project, ProjectForm, Scms}
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

class ProjectsController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(page: Int = 0) = Identified.async { implicit request =>
    for {
      projects <- dependencyClient(request).projects.get(
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(
        views.html.projects.index(
          uiData(request),
          PaginatedCollection(page, projects)
        )
      )
    }
  }

  def show(guid: UUID, languagesPage: Int = 0, librariesPage: Int = 0) = Identified.async { implicit request =>
    withProject(request, guid) { project =>
      for {
        libraryRecommendations <- dependencyClient(request).libraryRecommendations.getRecommendationsAndLibrariesAndProjectsByProjectGuid(project.guid)
        languages <- dependencyClient(request).languageVersions.get(
          projectGuid = Some(guid),
          limit = Pagination.DefaultLimit+1,
          offset = languagesPage * Pagination.DefaultLimit
        )
        libraries <- dependencyClient(request).libraryVersions.get(
          projectGuid = Some(guid),
          limit = Pagination.DefaultLimit+1,
          offset = librariesPage * Pagination.DefaultLimit
        )
      } yield {
        Ok(
          views.html.projects.show(
            uiData(request),
            project,
            libraryRecommendations,
            PaginatedCollection(languagesPage, languages),
            PaginatedCollection(librariesPage, libraries)
          )
        )
      }
    }
  }

  def create() = Identified { implicit request =>
    Ok(
      views.html.projects.create(
        uiData(request), ProjectsController.uiForm
      )
    )
  }

  def postCreate() = Identified.async { implicit request =>
    val boundForm = ProjectsController.uiForm.bindFromRequest
    boundForm.fold (

      formWithErrors => Future {
        Ok(views.html.projects.create(uiData(request), formWithErrors))
      },

      uiForm => {
        dependencyClient(request).projects.post(
          projectForm = ProjectForm(
            name = uiForm.name,
            scms = Scms(uiForm.scms),
            uri = uiForm.uri
          )
        ).map { project =>
          Redirect(routes.ProjectsController.show(project.guid)).flashing("success" -> "Project created")
        }.recover {
          case response: com.bryzek.dependency.v0.errors.ErrorsResponse => {
            Ok(views.html.projects.create(uiData(request), boundForm, response.errors.map(_.message)))
          }
        }
      }
    )
  }

  def edit(guid: UUID) = Identified.async { implicit request =>
    withProject(request, guid) { project =>
      Future {
        Ok(
          views.html.projects.edit(
            uiData(request), project, ProjectsController.uiForm.fill(
              ProjectsController.UiForm(
                name = project.name,
                scms = project.scms.toString,
                uri = project.uri
              )
            )
          )
        )
      }
    }
  }

  def postEdit(guid: UUID) = Identified.async { implicit request =>
    withProject(request, guid) { project =>
      val boundForm = ProjectsController.uiForm.bindFromRequest
      boundForm.fold (

        formWithErrors => Future {
          Ok(views.html.projects.edit(uiData(request), project, formWithErrors))
        },

        uiForm => {
          dependencyClient(request).projects.putByGuid(
            project.guid,
            ProjectForm(
              name = uiForm.name,
              scms = Scms(uiForm.scms),
              uri = uiForm.uri
            )
          ).map { project =>
            Redirect(routes.ProjectsController.show(project.guid)).flashing("success" -> "Project updated")
          }.recover {
            case response: com.bryzek.dependency.v0.errors.ErrorsResponse => {
              Ok(views.html.projects.edit(uiData(request), project, boundForm, response.errors.map(_.message)))
            }
          }
        }
      )
    }
  }

  def withProject[T](
    request: IdentifiedRequest[T],
    guid: UUID
  )(
    f: Project => Future[Result]
  ) = {
    dependencyClient(request).projects.getByGuid(guid).flatMap { project =>
      f(project)
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.ProjectsController.index()).flashing("warning" -> s"Project not found")
      }
    }
  }

}

object ProjectsController {

  case class UiForm(
    name: String,
    scms: String,
    uri: String
  )

  private val uiForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "scms" -> nonEmptyText,
      "uri" -> nonEmptyText
    )(UiForm.apply)(UiForm.unapply)
  )

}

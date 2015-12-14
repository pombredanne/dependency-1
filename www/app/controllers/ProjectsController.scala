package controllers

import com.bryzek.dependency.v0.errors.UnitResponse
import com.bryzek.dependency.v0.models.{Organization, Project, ProjectForm, Scms, SyncEvent, Visibility}
import com.bryzek.dependency.www.lib.DependencyClientProvider
import io.flow.user.v0.models.User
import io.flow.play.clients.UserTokensClient
import io.flow.play.util.{Pagination, PaginatedCollection}
import java.util.UUID
import java.util.concurrent.TimeUnit
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

  override def section = Some(com.bryzek.dependency.www.lib.Section.Projects)

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

  def show(guid: UUID, binariesPage: Int = 0, librariesPage: Int = 0) = Identified.async { implicit request =>
    withProject(request, guid) { project =>
      for {
        libraryRecommendations <- dependencyClient(request).libraryRecommendations.getRecommendationsAndLibrariesAndProjectsByProjectGuid(project.guid)
        binaryRecommendations <- dependencyClient(request).binaryRecommendations.getRecommendationsAndBinariesAndProjectsByProjectGuid(project.guid)
        binaries <- dependencyClient(request).binaryVersions.get(
          projectGuid = Some(guid),
          limit = Pagination.DefaultLimit+1,
          offset = binariesPage * Pagination.DefaultLimit
        )
        libraries <- dependencyClient(request).libraryVersions.get(
          projectGuid = Some(guid),
          limit = Pagination.DefaultLimit+1,
          offset = librariesPage * Pagination.DefaultLimit
        )
        watches <- dependencyClient(request).watchProjects.getWatchesAndProjects(
          userGuid = Some(request.user.guid),
          projectGuid = Some(guid)
        )
      } yield {
        Ok(
          views.html.projects.show(
            uiData(request),
            project,
            libraryRecommendations,
            binaryRecommendations,
            PaginatedCollection(binariesPage, binaries),
            PaginatedCollection(librariesPage, libraries),
            isWatching = !watches.isEmpty
          )
        )
      }
    }
  }

  def github(repositoriesPage: Int = 0) = Identified.async { implicit request =>
    for {
      org <- userOrg(request)
      repositories <- dependencyClient(request).repositories.getGithub(
        organizationGuid = Some(org.guid),
        existingProject = Some(false),
        limit = Pagination.DefaultLimit+1,
        offset = repositoriesPage * Pagination.DefaultLimit
      )
    } yield {
      Ok(
        views.html.projects.github(
          uiData(request), PaginatedCollection(repositoriesPage, repositories)
        )
      )
    }
  }

  def postGithub(
    name: String,
    repositoriesPage: Int = 0
  ) = Identified.async { implicit request =>
    userOrg(request).flatMap { org =>
      dependencyClient(request).repositories.getGithub(
        organizationGuid = Some(org.guid),
        name = Some(name)
      ).flatMap { selected =>
        dependencyClient(request).repositories.getGithub(
          organizationGuid = Some(org.guid),
          existingProject = Some(false),
          limit = Pagination.DefaultLimit+1,
          offset = repositoriesPage * Pagination.DefaultLimit
        ).flatMap { repositories =>
          selected.headOption match {
            case None => Future {
              Ok(
                views.html.projects.github(
                  uiData(request),
                  PaginatedCollection(repositoriesPage, repositories),
                  Seq("Repository with selected name was not found")
                )
              )
            }
            case Some(repo) => {
              dependencyClient(request).projects.post(
                ProjectForm(
                  organizationGuid = org.guid,
                  name = repo.name,
                  scms = Scms.Github,
                  visibility = repo.visibility,
                  uri = repo.uri
                )
              ).map { project =>
                Redirect(routes.ProjectsController.sync(project.guid)).flashing("success" -> "Project added")
              }.recover {
                case response: com.bryzek.dependency.v0.errors.ErrorsResponse => {
                  Ok(
                    views.html.projects.github(
                      uiData(request),
                      PaginatedCollection(repositoriesPage, repositories),
                      response.errors.map(_.message)
                    )
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  def create() = Identified.async { implicit request =>
    dependencyClient(request).organizations.get(
      userGuid = Some(request.user.guid),
      limit = 100
    ).map { orgs =>
      Ok(
        views.html.projects.create(
          uiData(request),
          orgs,
          ProjectsController.uiForm
        )
      )
    }
  }

  def postCreate() = Identified.async { implicit request =>
    val boundForm = ProjectsController.uiForm.bindFromRequest
    boundForm.fold (

      formWithErrors => Future {
        Ok(views.html.projects.create(uiData(request), formWithErrors))
      },

      uiForm => {
        userOrg(request).flatMap { org =>
          dependencyClient(request).projects.post(
            projectForm = ProjectForm(
              organizationGuid = org.guid,
              name = uiForm.name,
              scms = Scms(uiForm.scms),
              visibility = Visibility(uiForm.visibility),
              uri = uiForm.uri
            )
          ).map { project =>
            Redirect(routes.ProjectsController.sync(project.guid)).flashing("success" -> "Project created")
          }.recover {
            case response: com.bryzek.dependency.v0.errors.ErrorsResponse => {
              Ok(views.html.projects.create(uiData(request), boundForm, response.errors.map(_.message)))
            }
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
                visibility = project.visibility.toString,
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
              organizationGuid = project.organization.guid,
              name = uiForm.name,
              scms = Scms(uiForm.scms),
              visibility = Visibility(uiForm.visibility),
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

  def postDelete(guid: UUID) = Identified.async { implicit request =>
    dependencyClient(request).projects.deleteByGuid(guid).map { response =>
      Redirect(routes.ProjectsController.index()).flashing("success" -> s"Project deleted")
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.ProjectsController.index()).flashing("warning" -> s"Project not found")
      }
    }
  }

  /**
    * Waits for the latest sync to complete for this project.
    */
  def sync(guid: UUID, n: Int) = Identified.async { implicit request =>
    for {
      syncs <- dependencyClient(request).syncs.get(
        objectGuid = Some(guid)
      )
    } yield {
      val nextN = (n * 1.1).toInt match {
        case `n` => n + 1
        case other => other
      }

      syncs.find { _.event == SyncEvent.Completed } match {
        case Some(rec) => {
          Redirect(routes.ProjectsController.show(guid))
        }
        case None => {
          Ok(
            views.html.projects.sync(
              uiData(request),
              guid,
              nextN,
              syncs.find { _.event == SyncEvent.Started }
            )
          )
        }
      }
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
    visibility: String,
    uri: String
  )

  private val uiForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "scms" -> nonEmptyText,
      "visibility" -> nonEmptyText,
      "uri" -> nonEmptyText
    )(UiForm.apply)(UiForm.unapply)
  )

}

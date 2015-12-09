package controllers

import com.bryzek.dependency.v0.models.WatchProjectForm
import com.bryzek.dependency.lib.DependencyClientProvider
import io.flow.play.clients.UserTokensClient
import java.util.UUID

import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._

class WatchProjectsController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def section = None

  def create(projectGuid: UUID) = Identified.async { implicit request =>
    for {
      result <- dependencyClient(request).watchProjects.postWatchesAndProjects(
        WatchProjectForm(
          userGuid = request.user.guid,
          projectGuid = projectGuid
        )
      )
    } yield {
      Redirect(routes.ProjectsController.show(projectGuid)).flashing("success" -> "You are now watching this project")
    }
  }

  def delete(projectGuid: UUID) = Identified.async { implicit request =>
    dependencyClient(request).watchProjects.getWatchesAndProjects(
      userGuid = Some(request.user.guid),
      projectGuid = Some(projectGuid)
    ).map { watches =>
      watches.map { watch =>
        dependencyClient(request).watchProjects.deleteWatchesAndProjectsByGuid(watch.guid)
      }
      Redirect(routes.ProjectsController.show(projectGuid)).flashing("success" -> "You are no longer watching this project")
    }
  }

}


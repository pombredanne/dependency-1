package controllers

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import db.{Authorization, ProjectsDao}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import play.api.mvc._
import play.api.libs.json._

@javax.inject.Singleton
class GithubWebhooks @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController with Helpers {

  def postByProjectId(projectId: String) = Action(parse.json) { request =>
    ProjectsDao.findById(Authorization.All, projectId) match {
      case None => {
        NotFound
      }
      case Some(project) => {
        play.api.Logger.info(s"Received github webook for project[${project.id}]")
        MainActor.ref ! MainActor.Messages.ProjectSync(project.id)
        Ok(Json.toJson(Map("result" -> "success")))
      }
    }
  }

}

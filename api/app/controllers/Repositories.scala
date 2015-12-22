package controllers

import db.{Authorization, ProjectsDao}
import com.bryzek.dependency.api.lib.Github
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import io.flow.user.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.Future
import java.util.UUID

class Repositories @javax.inject.Inject() (
  val userTokensClient: UserTokensClient,
  val github: Github
) extends Controller with IdentifiedRestController {

  import scala.concurrent.ExecutionContext.Implicits.global

  def getGithub(
    name: Option[String] = None,
    organizationGuid: Option[UUID] = None,
    existingProject: Option[Boolean] = None,
    limit: Long = 25,
    offset: Long = 0
  ) = Identified.async { request =>
    if (!existingProject.isEmpty && organizationGuid.isEmpty) {
      Future {
        UnprocessableEntity(Json.toJson(Validation.error("When filtering by existing projects, you must also provide the organization_guid")))
      }
    } else {
      val auth = Authorization.User(request.user.guid)
      github.repositories(request.user).map { repos =>
        Ok(
          Json.toJson(
            repos.
              filter { r => name.isEmpty || name == Some(r.name) }.
              filter { r =>
                organizationGuid match {
                  case None => true
                  case Some(guid) => {
                    existingProject.isEmpty ||
                    existingProject == Some(true) && !ProjectsDao.findByOrganizationGuidAndName(auth, guid, r.name).isEmpty ||
                    existingProject == Some(false) && ProjectsDao.findByOrganizationGuidAndName(auth, guid, r.name).isEmpty
                  }
                }
              }.
              drop(offset.toInt).
              take(limit.toInt)
          )
        )
      }
    }
  }

}

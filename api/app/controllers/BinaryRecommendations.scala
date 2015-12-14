package controllers

import db.BinaryRecommendationsDao
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class BinaryRecommendations @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController with Helpers {

  def getRecommendationsAndBinariesAndProjectsByProjectGuid(
    projectGuid: UUID
  ) = Identified { request =>
    withProject(request.user, projectGuid) { project =>
      Ok(
        Json.toJson(
          BinaryRecommendationsDao.forProject(project)
        )
      )
    }
  }

}

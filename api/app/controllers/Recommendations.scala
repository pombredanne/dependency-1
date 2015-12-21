package controllers

import db.{Authorization, RecommendationsDao}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import com.bryzek.dependency.v0.models.RecommendationType
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Recommendations @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController with Helpers {

  def get(
    projectGuid: Option[UUID],
    `type`: Option[RecommendationType],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        RecommendationsDao.findAll(
          Authorization.User(request.user.guid),
          userGuid = Some(request.user.guid),
          projectGuid = projectGuid,
          `type` = `type`,
          limit = Some(limit),
          offset = offset
        )
      )
    )
  }

}

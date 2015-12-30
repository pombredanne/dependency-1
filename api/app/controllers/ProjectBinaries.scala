package controllers

import db.{Authorization, ProjectBinariesDao}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class ProjectBinaries @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController with Helpers {

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    projectGuid: Option[_root_.java.util.UUID],
    binaryGuid: Option[_root_.java.util.UUID],
    isSynced: Option[Boolean],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        ProjectBinariesDao.findAll(
          Authorization.User(request.user.id),
          guid = guid,
          guids = optionals(guids),
          projectGuid = projectGuid,
          binaryGuid = binaryGuid,
          isSynced = isSynced,
          limit = limit,
          offset = offset
        )
      )
    )
  }

}

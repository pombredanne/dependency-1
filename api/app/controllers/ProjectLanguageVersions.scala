package controllers

import db.ProjectBinaryVersionsDao
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class ProjectBinaryVersions @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  def get(
    projectGuid: Option[UUID],
    binaryGuid: Option[UUID],
    binaryVersionGuid: Option[UUID],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        ProjectBinaryVersionsDao.findAll(
          projectGuid = projectGuid,
          binaryGuid = binaryGuid,
          binaryVersionGuid = binaryVersionGuid,
          limit = limit,
          offset = offset
        )
      )
    )
  }

}

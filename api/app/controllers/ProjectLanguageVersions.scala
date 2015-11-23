package controllers

import db.ProjectLanguageVersionsDao
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class ProjectLanguageVersions @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  def get(
    projectGuid: Option[UUID],
    languageGuid: Option[UUID],
    languageVersionGuid: Option[UUID],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        ProjectLanguageVersionsDao.findAll(
          projectGuid = projectGuid,
          languageGuid = languageGuid,
          languageVersionGuid = languageVersionGuid,
          limit = limit,
          offset = offset
        )
      )
    )
  }

}

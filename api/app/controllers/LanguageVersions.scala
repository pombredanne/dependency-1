package controllers

import db.LanguageVersionsDao
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import com.bryzek.dependency.v0.models.LanguageVersion
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class LanguageVersions @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    languageGuid: Option[UUID],
    projectGuid: Option[UUID],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        LanguageVersionsDao.findAll(
          guid = guid,
          guids = optionalGuids(guids),
          languageGuid = languageGuid,
          projectGuid = projectGuid,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withLanguageVersion(guid) { language =>
      Ok(Json.toJson(language))
    }
  }

  def withLanguageVersion(guid: UUID)(
    f: LanguageVersion => Result
  ): Result = {
    LanguageVersionsDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(languageVersion) => {
        f(languageVersion)
      }
    }
  }
}


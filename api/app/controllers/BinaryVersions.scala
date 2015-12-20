package controllers

import db.{Authorization, BinaryVersionsDao}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.user.v0.models.User
import io.flow.play.util.Validation
import com.bryzek.dependency.v0.models.BinaryVersion
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class BinaryVersions @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    binaryGuid: Option[UUID],
    projectGuid: Option[UUID],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        BinaryVersionsDao.findAll(
          Authorization.User(request.user.guid),
          guid = guid,
          guids = optionalGuids(guids),
          binaryGuid = binaryGuid,
          projectGuid = projectGuid,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withBinaryVersion(request.user, guid) { binary =>
      Ok(Json.toJson(binary))
    }
  }

  def withBinaryVersion(user: User, guid: UUID)(
    f: BinaryVersion => Result
  ): Result = {
    BinaryVersionsDao.findByGuid(Authorization.User(user.guid), guid) match {
      case None => {
        NotFound
      }
      case Some(binaryVersion) => {
        f(binaryVersion)
      }
    }
  }
}


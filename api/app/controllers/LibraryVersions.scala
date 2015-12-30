package controllers

import db.{Authorization, LibraryVersionsDao}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.user.v0.models.User
import io.flow.play.util.Validation
import com.bryzek.dependency.v0.models.LibraryVersion
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class LibraryVersions @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    libraryGuid: Option[UUID],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        LibraryVersionsDao.findAll(
          Authorization.User(request.user.id),
          guid = guid,
          guids = optionals(guids),
          libraryGuid = libraryGuid,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withLibraryVersion(request.user, guid) { library =>
      Ok(Json.toJson(library))
    }
  }

  def withLibraryVersion(user: User, guid: UUID) (
    f: LibraryVersion => Result
  ): Result = {
    LibraryVersionsDao.findByGuid(
      Authorization.User(user.id),
      guid
    ) match {
      case None => {
        NotFound
      }
      case Some(libraryVersion) => {
        f(libraryVersion)
      }
    }
  }
}


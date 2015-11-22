package controllers

import db.LibraryVersionsDao
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
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
    projectGuid: Option[UUID],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        LibraryVersionsDao.findAll(
          guid = guid,
          guids = optionalGuids(guids),
          libraryGuid = libraryGuid,
          projectGuid = projectGuid,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withLibraryVersion(guid) { library =>
      Ok(Json.toJson(library))
    }
  }

  def withLibraryVersion(guid: UUID)(
    f: LibraryVersion => Result
  ): Result = {
    LibraryVersionsDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(libraryVersion) => {
        f(libraryVersion)
      }
    }
  }
}


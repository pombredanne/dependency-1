package controllers

import db.LibrariesDao
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import com.bryzek.dependency.v0.models.{Library, LibraryForm}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Libraries @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    projectGuid: Option[UUID],
    groupId: Option[String],
    artifactId: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        LibrariesDao.findAll(
          guid = guid,
          guids = optionalGuids(guids),
          projectGuid = projectGuid,
          groupId = groupId,
          artifactId = artifactId,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withLibrary(guid) { library =>
      Ok(Json.toJson(library))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[LibraryForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[LibraryForm] => {
        LibrariesDao.create(request.user, s.get) match {
          case Left(errors) => Conflict(Json.toJson(Validation.errors(errors)))
          case Right(library) => Created(Json.toJson(library))
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Identified { request =>
    withLibrary(guid) { library =>
      LibrariesDao.softDelete(request.user, library)
      NoContent
    }
  }

  def withLibrary(guid: UUID)(
    f: Library => Result
  ): Result = {
    LibrariesDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(library) => {
        f(library)
      }
    }
  }
}

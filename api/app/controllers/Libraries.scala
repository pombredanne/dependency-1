package controllers

import db.LibrariesDao
import io.flow.play.clients.UserTokensClient
import io.flow.common.v0.models.Error
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.{Validation, ValidatedForm}
import com.bryzek.dependency.v0.models.{AuthenticationForm, Library, LibraryForm}
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
          groupId = groupId,
          artifactId = artifactId,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    LibrariesDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(library) => {
        Ok(Json.toJson(library))
      }
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[LibraryForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[LibraryForm] => {
        val form = s.get
        LibrariesDao.validate(form) match {
          case valid @ ValidatedForm(_, Nil) => {
            val library = LibrariesDao.create(request.user, valid)
            Created(Json.toJson(library))
          }
          case invalid @ ValidatedForm(_, _) => {
            Conflict(Json.toJson(invalid.errors))
          }
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Identified { request =>
    LibrariesDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(library) => {
        LibrariesDao.softDelete(request.user, library)
        NoContent
      }
    }
  }

}

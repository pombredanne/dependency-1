package controllers

import db.{Authorization, BinariesDao}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import io.flow.user.v0.models.User
import com.bryzek.dependency.v0.models.{Binary, BinaryForm}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Binaries @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    projectGuid: Option[UUID],
    name: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        BinariesDao.findAll(
          Authorization.User(request.user.guid),
          guid = guid,
          guids = optionals(guids),
          projectGuid = projectGuid,
          name = name,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withBinary(request.user, guid) { binary =>
      Ok(Json.toJson(binary))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[BinaryForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[BinaryForm] => {
        val form = s.get
        BinariesDao.create(request.user, form) match {
          case Left(errors) => Conflict(Json.toJson(Validation.errors(errors)))
          case Right(binary) => Created(Json.toJson(binary))
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Identified { request =>
    withBinary(request.user, guid) { binary =>
      BinariesDao.softDelete(request.user, binary)
      NoContent
    }
  }

  def withBinary(user: User, guid: UUID)(
    f: Binary => Result
  ): Result = {
    BinariesDao.findByGuid(Authorization.User(user.guid), guid) match {
      case None => {
        NotFound
      }
      case Some(binary) => {
        f(binary)
      }
    }
  }



}

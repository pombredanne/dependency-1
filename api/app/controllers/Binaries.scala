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

@javax.inject.Singleton
class Binaries @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  def get(
    id: Option[String],
    ids: Option[Seq[String]],
    projectId: Option[String],
    name: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        BinariesDao.findAll(
          Authorization.User(request.user.id),
          id = id,
          ids = optionals(ids),
          projectId = projectId,
          name = name,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getById(id: String) = Identified { request =>
    withBinary(request.user, id) { binary =>
      Ok(Json.toJson(binary))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[BinaryForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[BinaryForm] => {
        val form = s.get
        BinariesDao.create(request.user, form) match {
          case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
          case Right(binary) => Created(Json.toJson(binary))
        }
      }
    }
  }

  def deleteById(id: String) = Identified { request =>
    withBinary(request.user, id) { binary =>
      BinariesDao.softDelete(request.user, binary)
      NoContent
    }
  }

  def withBinary(user: User, id: String)(
    f: Binary => Result
  ): Result = {
    BinariesDao.findById(Authorization.User(user.id), id) match {
      case None => {
        NotFound
      }
      case Some(binary) => {
        f(binary)
      }
    }
  }



}

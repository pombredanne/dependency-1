package controllers

import db.UsersDao
import io.flow.common.v0.models.Error
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import io.flow.user.v0.models.{User, UserForm}
import io.flow.user.v0.models.json._
import com.bryzek.dependency.v0.models.AuthenticationForm
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID
import scala.concurrent.Future

class Users @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(
    guid: Option[UUID],
    email: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Anonymous { request =>
    Ok(
      Json.toJson(
        UsersDao.findAll(
          guid = guid,
          email = email,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Anonymous { request =>
    withUser(guid) { user =>
      Ok(Json.toJson(user))
    }
  }

  def post() = Anonymous.async(parse.json) { request =>
    request.body.validate[UserForm] match {
      case e: JsError => Future {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[UserForm] => {
        request.user.map { userOption =>
          UsersDao.create(userOption, s.get) match {
            case Left(errors) => Conflict(Json.toJson(Validation.errors(errors)))
            case Right(user) => Created(Json.toJson(user))
          }
        }
      }
    }
  }

  def postAuthenticate() = Action(parse.json) { request =>
    request.body.validate[AuthenticationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[AuthenticationForm] => {
        val form = s.get
        UsersDao.findByEmail(form.email) match {
          case None => Conflict(
            Json.toJson(
              Seq(
                Error(
                  Validation.Codes.UserAuthorizationFailed,
                  "Email address not found"
                )
              )
            )
          )
          case Some(u) => {
            Ok(Json.toJson(u))
          }
        }
      }
    }
  }

  def withUser(guid: UUID)(
    f: User => Result
  ) = {
    UsersDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(user) => {
        f(user)
      }
    }
  }

}

package controllers

import db.UsersDao
import io.flow.common.v0.models.Error
import io.flow.play.util.Validation
import io.flow.user.v0.models.{User, UserForm}
import io.flow.user.v0.models.json._
import com.bryzek.dependency.v0.models.AuthenticationForm
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

class Users @javax.inject.Inject() () extends Controller {

  def get(
    guid: Option[UUID],
    email: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Action { request =>
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

  def getByGuid(guid: UUID) = Action { request =>
    UsersDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(user) => {
        Ok(Json.toJson(user))
      }
    }
  }

  // TODO: Pass in user
  def post() = Action(parse.json) { request =>
    request.body.validate[UserForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[UserForm] => {
        UsersDao.create(None, s.get) match {
          case Left(errors) => Conflict(Json.toJson(Validation.errors(errors)))
          case Right(user) => Created(Json.toJson(user))
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

}

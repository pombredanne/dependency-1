package controllers

import db.{UserIdentifiersDao, UsersDao}
import io.flow.common.v0.models.Error
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import io.flow.user.v0.models.{User, UserForm}
import io.flow.user.v0.models.json._
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
    identifier: Option[String]
  ) = Identified { request =>
    Ok(
      Json.toJson(
        UsersDao.findAll(
          guid = guid,
          email = email,
          identifier = identifier,
          limit = 1,
          offset = 0
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withUser(guid) { user =>
      Ok(Json.toJson(user))
    }
  }

  def getIdentifierByGuid(guid: UUID) = Identified { request =>
    withUser(guid) { user =>
      Ok(Json.toJson(UserIdentifiersDao.latestForUser(request.user, user)))
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

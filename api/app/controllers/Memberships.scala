package controllers

import db.{Authorization, MembershipsDao}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import io.flow.user.v0.models.User
import com.bryzek.dependency.v0.models.{Membership, MembershipForm, Role}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Memberships @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController with Helpers {

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    organization: Option[String],
    userGuid: Option[UUID],
    role: Option[Role],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        MembershipsDao.findAll(
          Authorization.User(request.user.id),
          guid = guid,
          guids = optionals(guids),
          organization = organization,
          userGuid = userGuid,
          role = role,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withMembership(request.user, guid) { membership =>
      Ok(Json.toJson(membership))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[MembershipForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[MembershipForm] => {
        MembershipsDao.create(request.user, s.get) match {
          case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
          case Right(membership) => Created(Json.toJson(membership))
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Identified { request =>
    withMembership(request.user, guid) { membership =>
      MembershipsDao.softDelete(request.user, membership)
      NoContent
    }
  }

  def withMembership(user: User, guid: UUID)(
    f: Membership => Result
  ): Result = {
    MembershipsDao.findByGuid(Authorization.User(user.id), guid) match {
      case None => {
        Results.NotFound
      }
      case Some(membership) => {
        f(membership)
      }
    }
  }

}

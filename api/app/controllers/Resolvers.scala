package controllers

import db.{Authorization, ResolversDao}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import com.bryzek.dependency.v0.models.{Resolver, ResolverForm, Visibility}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Resolvers @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController with Helpers {

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    org: Option[String],
    visibility: Option[Visibility],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        ResolversDao.findAll(
          Authorization.User(request.user.guid),
          guid = guid,
          guids = optionals(guids),
          visibility = visibility,
          org = org,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withResolver(request.user, guid) { resolver =>
      Ok(Json.toJson(resolver))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[ResolverForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[ResolverForm] => {
        ResolversDao.create(request.user, s.get) match {
          case Left(errors) => Conflict(Json.toJson(Validation.errors(errors)))
          case Right(resolver) => Created(Json.toJson(resolver))
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Identified { request =>
    withResolver(request.user, guid) { resolver =>
      ResolversDao.softDelete(request.user, resolver)
      NoContent
    }
  }

}

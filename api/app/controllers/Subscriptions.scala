package controllers

import db.SubscriptionsDao
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import com.bryzek.dependency.v0.models.{Publication, Subscription, SubscriptionForm}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Subscriptions @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    userGuid: Option[UUID],
    identifier: Option[String],
    publication: Option[Publication],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        SubscriptionsDao.findAll(
          guid = guid,
          guids = optionalGuids(guids),
          userGuid = userGuid,
          identifier = identifier,
          publication = publication,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withSubscription(guid) { subscription =>
      Ok(Json.toJson(subscription))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[SubscriptionForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[SubscriptionForm] => {
        val form = s.get
        SubscriptionsDao.create(request.user, form) match {
          case Left(errors) => Conflict(Json.toJson(Validation.errors(errors)))
          case Right(subscription) => Created(Json.toJson(subscription))
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Identified { request =>
    withSubscription(guid) { subscription =>
      SubscriptionsDao.softDelete(request.user, subscription)
      NoContent
    }
  }

  def withSubscription(guid: UUID)(
    f: Subscription => Result
  ): Result = {
    SubscriptionsDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(subscription) => {
        f(subscription)
      }
    }
  }

}

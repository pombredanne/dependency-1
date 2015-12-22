package controllers

import db.{SubscriptionsDao, UsersDao}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import io.flow.user.v0.models.User
import com.bryzek.dependency.v0.models.{Publication, Subscription, SubscriptionForm}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.Logger
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@javax.inject.Singleton
class Subscriptions @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  /**
   * If we find an 'identifier' query string parameter, use that to
   * find the user and authenticate as that user.
   */
  override def user(
    session: Session,
    headers: Headers,
    path: String,
    queryString: Map[String, Seq[String]]
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[User]] = {
    queryString.get("identifier").getOrElse(Nil).toList match {
      case Nil => {
        super.user(session, headers, path, queryString)
      }
      case id :: Nil => {
        Future {
          UsersDao.findAll(identifier = Some(id), limit = 1).headOption
        }
      }
      case multiple => {
        Logger.warn(s"Multiple identifiers[${multiple.size}] found in request - assuming no User")
        Future { None }
      }
    }
  }

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
          guids = optionals(guids),
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

  def post(identifier: Option[String]) = Identified(parse.json) { request =>
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

  def deleteByGuid(guid: UUID, identifier: Option[String]) = Identified { request =>
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

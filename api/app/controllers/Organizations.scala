package controllers

import db.OrganizationsDao
import io.flow.common.v0.models.Error
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import com.bryzek.dependency.v0.models.{Organization, OrganizationForm}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID
import scala.concurrent.Future

class Organizations @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    key: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        OrganizationsDao.findAll(
          guid = guid,
          guids = optionalGuids(guids),
          key = key,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withOrganization(guid) { organization =>
      Ok(Json.toJson(organization))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[OrganizationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[OrganizationForm] => {
        OrganizationsDao.create(request.user, s.get) match {
          case Left(errors) => Conflict(Json.toJson(Validation.errors(errors)))
          case Right(organization) => Created(Json.toJson(organization))
        }
      }
    }
  }

}

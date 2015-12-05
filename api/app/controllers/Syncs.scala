package controllers

import db.SyncsDao
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import com.bryzek.dependency.v0.models.SyncEvent
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Syncs @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController with Helpers {

  def get(
    objectGuid: Option[UUID],
    event: Option[SyncEvent],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        SyncsDao.findAll(
          objectGuid = objectGuid,
          event = event,
          limit = limit,
          offset = offset
        )
      )
    )
  }

}

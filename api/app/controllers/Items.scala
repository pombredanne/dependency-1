package controllers

import db.ItemsDao
import io.flow.play.clients.UserTokensClient
import com.bryzek.dependency.v0.models.json._
import play.api.mvc._
import play.api.libs.json._

@javax.inject.Singleton
class Items @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with BaseIdentifiedController {

  def getSearch(
    q: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    // TODO: Parse query
    Ok(
      Json.toJson(
        ItemsDao.findAll(
          authorization(request),
          q = q,
          limit = limit,
          offset = offset
        )
      )
    )
  }

}

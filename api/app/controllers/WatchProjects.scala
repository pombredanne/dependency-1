package controllers

import db.WatchProjectsDao
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import com.bryzek.dependency.v0.models.{WatchProject, WatchProjectForm}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class WatchProjects @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  def getWatchesAndProjects(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    userGuid: Option[UUID],
    projectGuid: Option[UUID],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        WatchProjectsDao.findAll(
          guid = guid,
          guids = optionalGuids(guids),
          userGuid = userGuid,
          projectGuid = projectGuid,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getWatchesAndProjectsByGuid(guid: UUID) = Identified { request =>
    withWatchProject(guid) { watchProject =>
      Ok(Json.toJson(watchProject))
    }
  }

  def postWatchesAndProjects() = Identified(parse.json) { request =>
    request.body.validate[WatchProjectForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[WatchProjectForm] => {
        val form = s.get
        WatchProjectsDao.create(request.user, form) match {
          case Left(errors) => Conflict(Json.toJson(Validation.errors(errors)))
          case Right(watchProject) => Created(Json.toJson(watchProject))
        }
      }
    }
  }

  def deleteWatchesAndProjectsByGuid(guid: UUID) = Identified { request =>
    withWatchProject(guid) { watchProject =>
      WatchProjectsDao.softDelete(request.user, watchProject)
      NoContent
    }
  }

  def withWatchProject(guid: UUID)(
    f: WatchProject => Result
  ): Result = {
    WatchProjectsDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(watchProject) => {
        f(watchProject)
      }
    }
  }

}

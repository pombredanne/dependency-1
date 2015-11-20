package controllers

import db.ProjectsDao
import io.flow.play.clients.UserTokensClient
import io.flow.common.v0.models.Error
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.{Validation, ValidatedForm}
import com.bryzek.dependency.v0.models.{AuthenticationForm, Project, ProjectForm}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Projects @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController {

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    name: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        ProjectsDao.findAll(
          guid = guid,
          guids = optionalGuids(guids),
          name = name,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withProject(guid) { project =>
      Ok(Json.toJson(project))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[ProjectForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[ProjectForm] => {
        val form = s.get
        ProjectsDao.validate(form) match {
          case valid @ ValidatedForm(_, Nil) => {
            val project = ProjectsDao.create(request.user, valid)
            Created(Json.toJson(project))
          }
          case invalid @ ValidatedForm(_, _) => {
            Conflict(Json.toJson(invalid.errors))
          }
        }
      }
    }
  }

  def patchByGuid(guid: UUID) = TODO

  def putByGuid(guid: UUID) = Identified(parse.json) { request =>
    withProject(guid) { project =>
      request.body.validate[ProjectForm] match {
        case e: JsError => {
          Conflict(Json.toJson(Validation.invalidJson(e)))
        }
        case s: JsSuccess[ProjectForm] => {
          val form = s.get
          ProjectsDao.validate(form, Some(project)) match {
            case valid @ ValidatedForm(_, Nil) => {
              val updated = ProjectsDao.update(request.user, project, valid)
              Ok(Json.toJson(updated))
            }
            case invalid @ ValidatedForm(_, _) => {
              Conflict(Json.toJson(invalid.errors))
            }
          }
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Identified { request =>
    withProject(guid) { project =>
      ProjectsDao.softDelete(request.user, project)
      NoContent
    }
  }

  def withProject(guid: UUID)(
    f: Project => Result
  ): Result = {
    ProjectsDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(project) => {
        f(project)
      }
    }
  }

}

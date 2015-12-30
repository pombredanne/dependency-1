package controllers

import db.{Authorization, ProjectsDao}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import com.bryzek.dependency.v0.models.{Project, ProjectForm, ProjectPatchForm}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Projects @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController with Helpers {

  def get(
    guid: Option[UUID],
    guids: Option[Seq[UUID]],
    organization: Option[String],
    name: Option[String],
    groupId: _root_.scala.Option[String],
    artifactId: _root_.scala.Option[String],
    version: _root_.scala.Option[String],
    libraryGuid: _root_.scala.Option[_root_.java.util.UUID],
    binary: _root_.scala.Option[String],
    binaryGuid: _root_.scala.Option[_root_.java.util.UUID],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        ProjectsDao.findAll(
          Authorization.User(request.user.id),
          guid = guid,
          guids = optionals(guids),
          name = name,
          organization = organization,
          groupId = groupId,
          artifactId = artifactId,
          version = version,
          libraryGuid = libraryGuid,
          binary = binary,
          binaryGuid = binaryGuid,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
    withProject(request.user, guid) { project =>
      Ok(Json.toJson(project))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[ProjectForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[ProjectForm] => {
        ProjectsDao.create(request.user, s.get) match {
          case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
          case Right(project) => Created(Json.toJson(project))
        }
      }
    }
  }

  def patchByGuid(guid: UUID) = Identified(parse.json) { request =>
    withProject(request.user, guid) { project =>
      request.body.validate[ProjectPatchForm] match {
        case e: JsError => {
          UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
        }
        case s: JsSuccess[ProjectPatchForm] => {
          val patch = s.get
          val form = ProjectForm(
            organization = project.organization.key,  // do not support patch to move project to a new org
            name = patch.name.getOrElse(project.name),
            visibility = patch.visibility.getOrElse(project.visibility),
            scms = patch.scms.getOrElse(project.scms),
            uri = patch.uri.getOrElse(project.uri)
          )
          ProjectsDao.update(request.user, project, form) match {
            case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
            case Right(updated) => Ok(Json.toJson(updated))
          }
        }
      }
    }
  }

  def putByGuid(guid: UUID) = Identified(parse.json) { request =>
    withProject(request.user, guid) { project =>
      request.body.validate[ProjectForm] match {
        case e: JsError => {
          UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
        }
        case s: JsSuccess[ProjectForm] => {
          ProjectsDao.update(request.user, project, s.get) match {
            case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
            case Right(updated) => Ok(Json.toJson(updated))
          }
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Identified { request =>
    withProject(request.user, guid) { project =>
      ProjectsDao.softDelete(request.user, project)
      NoContent
    }
  }

}

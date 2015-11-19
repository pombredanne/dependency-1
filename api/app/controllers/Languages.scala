package controllers

import db.LanguagesDao
import io.flow.play.clients.UserTokensClient
import io.flow.common.v0.models.Error
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.{Validation, ValidatedForm}
import com.bryzek.dependency.v0.models.{AuthenticationForm, Language, LanguageForm}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Languages @javax.inject.Inject() (
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
        LanguagesDao.findAll(
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
    LanguagesDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(language) => {
        Ok(Json.toJson(language))
      }
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[LanguageForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[LanguageForm] => {
        val form = s.get
        LanguagesDao.validate(form) match {
          case valid @ ValidatedForm(_, Nil) => {
            val language = LanguagesDao.create(request.user, valid)
            Created(Json.toJson(language))
          }
          case invalid @ ValidatedForm(_, _) => {
            Conflict(Json.toJson(invalid.errors))
          }
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Identified { request =>
    LanguagesDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(language) => {
        LanguagesDao.softDelete(request.user, language)
        NoContent
      }
    }
  }

}

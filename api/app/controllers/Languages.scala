package controllers

import db.LanguagesDao
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
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
    withLanguage(guid) { language =>
      Ok(Json.toJson(language))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[LanguageForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[LanguageForm] => {
        val form = s.get
        LanguagesDao.create(request.user, form) match {
          case Left(errors) => Conflict(Json.toJson(Validation.errors(errors)))
          case Right(language) => Created(Json.toJson(language))
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Identified { request =>
    withLanguage(guid) { language =>
      LanguagesDao.softDelete(request.user, language)
      NoContent
    }
  }

  def withLanguage(guid: UUID)(
    f: Language => Result
  ): Result = {
    LanguagesDao.findByGuid(guid) match {
      case None => {
        NotFound
      }
      case Some(language) => {
        f(language)
      }
    }
  }



}

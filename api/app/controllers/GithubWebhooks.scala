package controllers

import com.bryzek.dependency.v0.models.GithubWebhookBody
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.json._
import db.{Authorization, ProjectsDao}
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.IdentifiedRestController
import io.flow.play.util.Validation
import play.api.mvc._
import play.api.libs.json._

@javax.inject.Singleton
class GithubWebhooks @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with IdentifiedRestController with Helpers {

  def postWebhooksAndGithub() = Action(parse.json) { request =>
    println("HOOKS POST")
    println(request.body.toString)

    request.body.validate[GithubWebhookBody] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[GithubWebhookBody] => {
        val body = s.get
        println("BODY: " + body.toString)
        println("HOOK ID[${body.hook.id}]")
        println("HOOK repo[${body.repository.fullName}]")
        println("HOOK sender[${body.sender}]")
        
        Ok(Json.toJson(Map("result" -> "success")))
      }
    }
  }

}

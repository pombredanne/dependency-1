package controllers

import io.flow.common.v0.models.Healthcheck
import io.flow.common.v0.models.json._

import play.api._
import play.api.mvc._
import play.api.libs.json._

class Healthchecks extends Controller {

  private val HealthyJson = Json.toJson(Healthcheck(status = "healthy"))

  def get() = Action { request =>
    com.bryzek.dependency.actors.MainActor.ref
    Ok(HealthyJson)
  }

}

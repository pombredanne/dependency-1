package controllers

import db.UsersDao
import com.bryzek.dependency.v0.models.Publication
import com.bryzek.dependency.api.lib.Person
import com.bryzek.dependency.actors._
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.AnonymousController
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Emails @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with AnonymousController with Helpers {

  override def user(
    session: play.api.mvc.Session,
    headers: play.api.mvc.Headers
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ) = scala.concurrent.Future { None }

  def get() = Action { request =>
    val user = UsersDao.findByEmail("mbryzek@alum.mit.edu").getOrElse {
      sys.error("User not found")
    }
    Ok(
      EmailMessage(Publication.DailySummary).generate(user.guid, Person(user.email.get,user.name))
    )
  }

}

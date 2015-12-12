package controllers

import db.UsersDao
import com.bryzek.dependency.v0.models.Publication
import com.bryzek.dependency.api.lib.Person
import com.bryzek.dependency.actors._
import io.flow.play.clients.UserTokensClient
import io.flow.play.controllers.AnonymousController
import io.flow.play.util.DefaultConfig
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@javax.inject.Singleton
class Emails @javax.inject.Inject() (
  val userTokensClient: UserTokensClient
) extends Controller with AnonymousController with Helpers {

  private[this] val TestEmailAddressName = "com.bryzek.dependency.test.email"
  private[this] lazy val TestEmailAddress = DefaultConfig.optionalString(TestEmailAddressName)

  override def user(
    session: play.api.mvc.Session,
    headers: play.api.mvc.Headers
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ) = scala.concurrent.Future { None }

  def get() = Action { request =>
    TestEmailAddress match {
      case None => Ok(s"Set the $TestEmailAddressName property to enable testing")
      case Some(email) => {
        UsersDao.findByEmail("mbryzek@alum.mit.edu") match {
          case None => Ok(s"No user with email address[$email] found")
          case Some(user) => {
            Ok(
              EmailMessage(Publication.DailySummary).generate(user.guid, Person(user.email.get,user.name))
            )
          }
        }
      }
    }
  }

}

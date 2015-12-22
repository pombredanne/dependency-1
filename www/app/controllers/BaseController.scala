package controllers

import com.bryzek.dependency.v0.Client
import com.bryzek.dependency.v0.models.Organization
import com.bryzek.dependency.www.lib.{DependencyClientProvider, Section, UiData}
import io.flow.play.clients.UserTokensClient
import io.flow.user.v0.models.User
import io.flow.play.controllers.IdentifiedController
import scala.concurrent.{ExecutionContext, Future}
import play.api._
import play.api.i18n._
import play.api.mvc._

object Helpers {

  def userFromSession(
    userTokensClient: UserTokensClient,
    session: play.api.mvc.Session
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): scala.concurrent.Future[Option[io.flow.user.v0.models.User]] = {
    session.get("user_guid") match {
      case None => {
        scala.concurrent.Future { None }
      }
      case Some(userGuid) => {
        println(s"get user by token($userGuid)")
        val user  = userTokensClient.getUserByToken(userGuid)
        println(s" -- get $user")
        user
      }
    }
  }

}

abstract class BaseController(
  val userTokensClient: UserTokensClient,
  val dependencyClientProvider: DependencyClientProvider
) extends Controller
    with IdentifiedController
    with I18nSupport
{

  def section: Option[Section]

  override def unauthorized[A](request: Request[A]): Result = {
    Redirect(routes.LoginController.index(return_url = Some(request.path))).flashing("warning" -> "Please login")
  }

  def organizations[T](
    request: IdentifiedRequest[T]
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[Seq[Organization]] = {
    dependencyClient(request).organizations.get(
      userGuid = Some(request.user.guid),
      limit = 100
    )
  }

  override def user(
    session: play.api.mvc.Session,
    headers: play.api.mvc.Headers,
    path: String,
    queryString: Map[String, Seq[String]]
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): scala.concurrent.Future[Option[io.flow.user.v0.models.User]] = {
    Helpers.userFromSession(userTokensClient, session)
  }

  def uiData[T](request: IdentifiedRequest[T]): UiData = {
    UiData(
      requestPath = request.path,
      user = Some(request.user),
      section = section
    )
  }

  def uiData[T](request: AnonymousRequest[T], user: Option[User]): UiData = {
    UiData(
      requestPath = request.path,
      user = user,
      section = section
    )
  }

  def dependencyClient[T](request: IdentifiedRequest[T]): Client = {
    dependencyClientProvider.newClient(user = Some(request.user))
  }

  /**
    * Temporary until we expose orgs in the UI
    */
  def userOrg[T](
    request: IdentifiedRequest[T]
  ) (
    implicit ec: ExecutionContext
  ): Future[Organization] = {
    dependencyClient(request).organizations.getUsersByUserGuid(request.user.guid)
  }

}

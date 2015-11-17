package controllers

import com.bryzek.dependency.lib.{DependencyClientProvider, UiData}
import com.bryzek.dependency.v0.models.{Library, Project, User}
import com.bryzek.dependency.v0.errors.UnitResponse
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import play.api.mvc._
import play.api.mvc.Results.Redirect
import play.api.Play.current
import java.util.UUID

case class RequestHelper[A](
  provider: DependencyClientProvider,
  request: Request[A]
) {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val pathParts = request.path.split("/").drop(1)

  lazy val project: Future[Option[Project]] = Future { None }

  lazy val library: Future[Option[Library]] = {
    if (pathParts.length >= 2 && pathParts(0) == "libraries") {
      val key = pathParts(1)
      println(s"library key[$key]")
      // provider.newClient(user = None).libraries.get(key = Some(key), limit = 1).map { _.headOption }
      Future { None }
    } else {
      Future { None }
    }
  }

  lazy val user: Future[Option[User]] = {
    request.session.get("user_guid") match {
      case None => Future {
        None
      }
      case Some(guid) => {
        provider.newClient(user = None).users.getByGuid(UUID.fromString(guid)).map { Some(_) }.recover {
          case UnitResponse(404) => None
        }
      }
    }
  }

}


class AuthenticatedRequest[A](
  val user: User,
  request: Request[A]
) extends WrappedRequest[A](request) {

  def uiData(
    title: Option[String] = None
  ) = com.bryzek.dependency.lib.UiData(
    requestPath = request.path,
    user = Some(user),
    title = title
  )

}


case class Authenticated(
  implicit val provider: DependencyClientProvider,
  implicit val ec: scala.concurrent.ExecutionContext
) extends ActionBuilder[AuthenticatedRequest] {

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
    RequestHelper(provider, request).user flatMap { result =>
      result match {
        case None => Future {
          Redirect(routes.LoginController.index(return_url = Some(request.path))).flashing("warning" -> s"Please login")
        }
        case Some(user) => {
          block(new AuthenticatedRequest(user, request))
        }
      }
    }
  }

}

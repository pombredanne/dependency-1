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

  lazy val project: Option[Project] = None

  lazy val library: Option[Library] = {
    if (pathParts.length >= 2 && pathParts(0) == "libraries") {
      val key = pathParts(1)
      println(s"library key[$key]")
      None
/*
      Await.result(
        api.libraries.get(teamKey).map { Some(_) }.recover {
          case UnitResponse(404) => None
        },
        1000.millis
      )
 */
    } else {
      None
    }
  }

  lazy val user: Future[Option[User]] = {
    request.session.get("user_guid") match {
      case None => Future {
        None
      }
      case Some(guid) => {
        println(s"GUID[$guid]")
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


case class AuthenticatedAction @javax.inject.Inject() (
  provider: DependencyClientProvider
) extends ActionBuilder[AuthenticatedRequest] {

  import scala.concurrent.ExecutionContext.Implicits.global

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

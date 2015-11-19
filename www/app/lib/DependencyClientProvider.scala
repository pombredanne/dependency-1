package com.bryzek.dependency.lib

import io.flow.play.clients.UserTokensClient
import io.flow.play.util.Config
import io.flow.user.v0.models.User
import com.bryzek.dependency.v0.{Authorization, Client}
import com.bryzek.dependency.v0.errors.UnitResponse
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait DependencyClientProvider extends UserTokensClient {

  def newClient(user: Option[User]): Client

}

@javax.inject.Singleton
class DefaultDependencyClientProvider() extends DependencyClientProvider {

  def host: String = Config.requiredString("dependency.api.host")
  def token: String = Config.requiredString("dependency.api.token")

  private[this] lazy val client = new Client(host)

  override def newClient(user: Option[User]): Client = {
    user match {
      case None => {
        client
      }
      case Some(u) => {
        new Client(
          apiUrl = host,
          auth = Some(
            Authorization.Basic(
              username = u.guid.toString,
              password = None
            )
          )
        )
      }
    }
  }

  override def getUserByToken(
    token: String
  )(implicit ec: ExecutionContext): Future[Option[User]] = {
    Try(UUID.fromString(token)) match {
      case Success(guid) => {
        client.users.getByGuid(guid).
          map {
            Some(_)
          }.
          recover {
            case UnitResponse(404) => None
          }
      }
      case Failure(_) => Future {
        None
      }
    }
  }


}

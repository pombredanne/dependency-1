package com.bryzek.dependency.www.lib

import io.flow.play.clients.UserTokensClient
import io.flow.play.util.DefaultConfig
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

  def host: String = DefaultConfig.requiredString("dependency.api.host")
  def token: String = DefaultConfig.requiredString("dependency.api.token")

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
        client.users.get(guid = Some(guid)).map { _.headOption }
      }
      case Failure(_) => Future {
        None
      }
    }
  }

}

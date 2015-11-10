package lib

import io.flow.user.v0.models.User
import io.flow.dependency.v0.{Authorization, Client}

trait SplashpageClient {

  def newClient(user: Option[User]): Client

}

@javax.inject.Singleton
class DefaultSplashpageClient() extends SplashpageClient {

  def host: String = Config.requiredString("dependency.api.host")
  def token: String = Config.requiredString("dependency.api.token")

  override def newClient(user: Option[User]): Client = {
    new Client(
      apiUrl = host,
      auth = Some(
        Authorization.Basic(
          username = token,
        password = None
        )
      )
    )
  }

}

package lib

import io.flow.play.util.Config
import com.bryzek.dependency.v0.models.User
import com.bryzek.dependency.v0.{Authorization, Client}

trait DependencyClientProvider {

  def newClient(user: Option[User]): Client

}

@javax.inject.Singleton
class DefaultDependencyClientProvider() extends DependencyClientProvider {

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

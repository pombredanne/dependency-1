package controllers

import io.flow.github.oauth.v0.Client
import io.flow.github.oauth.v0.models.AccessTokenForm
import io.flow.play.util.DefaultConfig
import com.bryzek.dependency.lib.DependencyClientProvider
import play.api._
import play.api.i18n._
import play.api.mvc._

class GithubCallbacksController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  val provider: DependencyClientProvider
) extends Controller with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val githubClientId = DefaultConfig.requiredString("github.dependency.client.id")
  private[this] lazy val githubClientSecret = DefaultConfig.requiredString("github.dependency.client.secret")
  private[this] lazy val githubOauthClient = new Client(
    apiUrl = "https://github.com",
    defaultHeaders = Seq(
      ("Accept" -> "application/json")
    )
  )

  def authorizations(code: String, state: Option[String]) = Action.async { implicit request =>
    // Content-Type: application/json
    // POST https://github.com/login/oauth/access_token
    githubOauthClient.accessTokens.postLoginAndOauthAndAccessToken(
      AccessTokenForm(
        clientId = githubClientId,
        clientSecret = githubClientSecret,
        code = code
      )
    ).map { result =>
      sys.error(s"result: " + result)
    }
  }

}

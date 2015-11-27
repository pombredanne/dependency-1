package controllers

import io.flow.github.oauth.v0.{Client => GithubOauthClient}
import io.flow.github.oauth.v0.models.AccessTokenForm
import io.flow.github.v0.{Client => GithubClient}
import io.flow.user.v0.models.{ExternalId, NameForm, System, UserForm}
import io.flow.play.util.DefaultConfig
import com.bryzek.dependency.lib.DependencyClientProvider
import play.api._
import play.api.i18n._
import play.api.mvc._
import scala.concurrent.Future

class GithubCallbacksController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  val provider: DependencyClientProvider
) extends Controller with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val client = provider.newClient(user = None)
  private[this] lazy val githubClientId = DefaultConfig.requiredString("github.dependency.client.id")
  private[this] lazy val githubClientSecret = DefaultConfig.requiredString("github.dependency.client.secret")

  private[this] lazy val githubOauthClient = new GithubOauthClient(
    apiUrl = "https://github.com",
    defaultHeaders = Seq(
      ("Accept" -> "application/json")
    )
  )

  private[this] def githubClient(oauthToken: String) = new GithubClient(
    apiUrl = "https://api.github.com",
    defaultHeaders = Seq(
      ("Authorization" -> s"token $oauthToken")
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
    ).flatMap { response =>
      githubClient(response.accessToken).users.getUser().flatMap { githubUser =>
        println(s"githubUser: " + githubUser)
        githubUser.email match {
          case None => {
            sys.error("Need email")
          }
          case Some(email) => {
            client.users.get(email = Some(email)).flatMap { users =>
              users.headOption match {
                case Some(user) => Future {
                  // TODO: Thread through returnUrl
                  Redirect(routes.ApplicationController.index()).withSession { "user_guid" -> user.guid.toString }
                }
                case None => {
                  client.users.post(
                    UserForm(
                      email = Some(email),
                      name = Some(
                        NameForm(
                          first = githubUser.name
                      )
                      ),
                      avatarUrl = githubUser.avatarUrl,
                      externalIds = Some(
                        Seq(ExternalId(System.Github, githubUser.id.toString))
                      )
                    )
                  ).map { user =>
                    // TODO: Thread through returnUrl
                    Redirect(routes.ApplicationController.index()).withSession { "user_guid" -> user.guid.toString }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

}

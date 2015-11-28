package com.bryzek.dependency.lib

import db.{GithubUsersDao, UsersDao}
import com.bryzek.dependency.v0.models.GithubUserForm
import io.flow.user.v0.models.{NameForm, User, UserForm}
import io.flow.play.util.DefaultConfig
import io.flow.github.oauth.v0.{Client => GithubOauthClient}
import io.flow.github.oauth.v0.models.AccessTokenForm
import io.flow.github.v0.{Client => GithubClient}
import io.flow.github.v0.models.{User => GithubUser}
import scala.concurrent.{ExecutionContext, Future}

trait Github {

  /**
    * Given an auth validation code, pings the github UI to access the
    * user data, upserts that user with the dependency database, and
    * returns the user (or a list of errors).
    * 
    * @param code The oauth authorization code from github
    */
  def getUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], User]] = {
    getGithubUserFromCode(code).map {
      case Left(errors) => Left(errors)
      case Right(githubUser) => {
        githubUser.email match {
          case None => {
            Left(Seq("Github account does not have an email address that we can read"))
          }
          case Some(email) => {
            val userResult: Either[Seq[String], User] = UsersDao.findByEmail(email) match {
              case Some(user) => Right(user)
              case None => {
                UsersDao.create(
                  createdBy = None,
                  form = UserForm(
                    email = Some(email),
                    name = Some(
                      NameForm(
                        first = githubUser.name
                      )
                    ),
                    avatarUrl = githubUser.avatarUrl
                  )
                )
              }
            }

            userResult match {
              case Left(errors) => {
                Left(errors)
              }
              case Right(user) => {
                GithubUsersDao.upsertByLogin(
                  createdBy = None,
                  form = GithubUserForm(
                    userGuid = user.guid,
                    id = githubUser.id,
                    login = githubUser.login
                  )
                )

                Right(user)
              }
            }
          }
        }
      }
    }
  }

  /**
    * Fetches github user from an oauth code
    */
  def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUser]]

}

@javax.inject.Singleton
class DefaultGithub @javax.inject.Inject() () extends Github {

  private[this] lazy val clientId = DefaultConfig.requiredString("github.dependency.client.id")
  private[this] lazy val clientSecret = DefaultConfig.requiredString("github.dependency.client.secret")

  private[this] lazy val oauthClient = new GithubOauthClient(
    apiUrl = "https://github.com",
    defaultHeaders = Seq(
      ("Accept" -> "application/json")
    )
  )

  private[this] def apiClient(oauthToken: String) = new GithubClient(
    apiUrl = "https://api.github.com",
    defaultHeaders = Seq(
      ("Authorization" -> s"token $oauthToken")
    )
  )

  override def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUser]] = {
    oauthClient.accessTokens.postLoginAndOauthAndAccessToken(
      AccessTokenForm(
        clientId = clientId,
        clientSecret = clientSecret,
        code = code
      )
    ).flatMap { response =>
      apiClient(response.accessToken).users.getUser().map { githubUser =>
        Right(githubUser)
      }
    }
  }

}

class MockGithub() extends Github {

  override def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUser]] = {
    Future {
      MockGithubData.getUser(code) match {
        case None => Left(Seq("Invalid access code"))
        case Some(u) => Right(u)
      }
    }
  }

}

object MockGithubData {

  var users = scala.collection.mutable.Map[String, GithubUser]()

  def addUser(code: String, user: GithubUser) {
    users +== (code -> user)
  }

  def getUser(code: String): Option[GithubUser] = {
    users.lift(code)
  }

}

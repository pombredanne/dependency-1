package com.bryzek.dependency.lib

import db.{GithubUsersDao, TokensDao, UsersDao}
import com.bryzek.dependency.v0.models.{GithubUserForm, Repository, TokenForm, Visibility}
import io.flow.user.v0.models.{NameForm, User, UserForm}
import io.flow.play.util.DefaultConfig
import io.flow.github.oauth.v0.{Client => GithubOauthClient}
import io.flow.github.oauth.v0.models.AccessTokenForm
import io.flow.github.v0.{Client => GithubClient}
import io.flow.github.v0.errors.UnitResponse
import io.flow.github.v0.models.{User => GithubUser}
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

case class GithubUserWithToken(
  user: GithubUser,
  token: String
)

trait Github {

  /**
    * Fetches the contents of the file at the specified path from the
    * given repository. Returns None if the file is not found.
    * 
    * @param path e.g. "build.sbt",  "project/plugins.sbt", etc.
    */
  def file(
    user: User,
    projectUri: String,
    path: String
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[String]]

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
      case Right(githubUserWithToken) => {
        githubUserWithToken.user.email match {
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
                        first = githubUserWithToken.user.name
                      )
                    ),
                    avatarUrl = githubUserWithToken.user.avatarUrl
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
                    id = githubUserWithToken.user.id,
                    login = githubUserWithToken.user.login
                  )
                )

                TokensDao.upsert(
                  createdBy = user,
                  form = TokenForm(
                    userGuid = user.guid,
                    tag = TokensDao.GithubOauthTokenTag,
                    token = githubUserWithToken.token
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
  def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUserWithToken]]

  def repositories(user: User)(implicit ec: ExecutionContext): Future[Seq[Repository]]

  /**
    * For this user, returns the oauth token if available
    */
  def oauthToken(user: User): Option[String]

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

  override def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUserWithToken]] = {
    oauthClient.accessTokens.postLoginAndOauthAndAccessToken(
      AccessTokenForm(
        clientId = clientId,
        clientSecret = clientSecret,
        code = code
      )
    ).flatMap { response =>
      apiClient(response.accessToken).users.getUser().map { githubUser =>
        Right(
          GithubUserWithToken(
            user = githubUser,
            token = response.accessToken
          )
        )
      }
    }
  }

  override def repositories(user: User)(implicit ec: ExecutionContext): Future[Seq[Repository]] = {
    oauthToken(user) match {
      case None => Future { Nil }
      case Some(token) => {
        apiClient(token).repositories.getUserAndRepos().map { repos =>
          repos.map { repo =>
            Repository(
              name = s"${repo.owner.login}/${repo.name}",
              visibility = if (repo.`private`) { Visibility.Private } else { Visibility.Public },
              uri = repo.htmlUrl
            )
          }
        }
      }
    }
  }

  override def oauthToken(user: User): Option[String] = {
    TokensDao.findByUserGuidAndTag(user.guid, TokensDao.GithubOauthTokenTag).map(_.token)
  }

  override def file(
    user: User,
    projectUri: String,
    path: String
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[String]] = {
    GithubUtil.parseUri(projectUri) match {
      case Left(error) => {
        sys.error(error)
      }
      case Right(repo) => {
        oauthToken(user) match {
          case None => Future {
            None
          }
          case Some(token) => {
            apiClient(token).contents.getReposByOwnerAndRepoAndPath(
              owner = repo.owner,
              repo = repo.project,
              path = path
            ).map { contents =>
              Some(GithubUtil.toText(contents))
            }.recover {
              case UnitResponse(404) => {
                None
              }
            }
          }
        }
      }
    }
  }

}

class MockGithub() extends Github {

  override def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUserWithToken]] = {
    Future {
      MockGithubData.getUserByCode(code) match {
        case None => Left(Seq("Invalid access code"))
        case Some(u) => Right(u)
      }
    }
  }

  override def repositories(user: User)(implicit ec: ExecutionContext): Future[Seq[Repository]] = {
    Future {
      MockGithubData.repositories(user)
    }
  }

  override def oauthToken(user: User): Option[String] = {
    MockGithubData.getToken(user)
  }

  override def file(
    user: User,
    projectUri: String,
    path: String
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[String]] = {
    Future {
      MockGithubData.getFile(projectUri, path)
    }
  }

}

object MockGithubData {

  private[this] var githubUserByCodes = scala.collection.mutable.Map[String, GithubUserWithToken]()
  private[this] var userTokens = scala.collection.mutable.Map[UUID, String]()
  private[this] var repositories = scala.collection.mutable.Map[UUID, Repository]()
  private[this] var files = scala.collection.mutable.Map[String, String]()

  def addUser(user: GithubUser, code: String, token: Option[String] = None) {
    githubUserByCodes +== (
      code -> GithubUserWithToken(
        user = user,
        token = token.getOrElse(UUID.randomUUID.toString)
      )
    )
  }

  def getUserByCode(code: String): Option[GithubUserWithToken] = {
    githubUserByCodes.lift(code)
  }

  def addUserOauthToken(token: String, user: User) {
    userTokens +== (user.guid -> token)
  }

  def getToken(user: User): Option[String] = {
    userTokens.lift(user.guid)
  }

  def addRepository(user: User, repository: Repository) = {
    repositories +== (user.guid -> repository)
  }

  def repositories(user: User): Seq[Repository] = {
    repositories.lift(user.guid) match {
      case None => Nil
      case Some(repo) => Seq(repo)
    }
  }

  def addFile(
    projectUri: String,
    path: String,
    contents: String
  ) {
    files +== (s"${projectUri}.$path" -> contents)
  }

  def getFile(
    projectUri: String,
    path: String
  ): Option[String] = {
    files.get(s"${projectUri}.$path")
  }


}

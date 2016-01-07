package com.bryzek.dependency.api.lib

import db.{GithubUsersDao, InternalTokenForm, TokensDao, UsersDao}
import com.bryzek.dependency.v0.models.{GithubUserForm, Repository, UserForm, Visibility}
import io.flow.common.v0.models.{Name, User}
import io.flow.play.util.{DefaultConfig, IdGenerator}
import io.flow.github.oauth.v0.{Client => GithubOauthClient}
import io.flow.github.oauth.v0.models.AccessTokenForm
import io.flow.github.v0.{Client => GithubClient}
import io.flow.github.v0.errors.UnitResponse
import io.flow.github.v0.models.{User => GithubUser}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger

case class GithubUserData(
  githubId: Long,
  login: String,
  token: String,
  emails: Seq[String],
  name: Option[String],
  avatarUrl: Option[String]
)

object GithubHelper {

  def parseName(value: String): Name = {
    if (value.trim.isEmpty) {
      Name()
    } else {
      value.trim.split("\\s+").toList match {
        case Nil => Name()
        case first :: Nil => Name(first = Some(first))
        case first :: last :: Nil => Name(first = Some(first), last = Some(last))
        case first :: multiple => Name(first = Some(first), last = Some(multiple.mkString(" ")))
      }
    }
  }

}

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
        val userResult: Either[Seq[String], User] = UsersDao.findByGithubUserId(githubUserWithToken.githubId) match {
          case Some(user) => {
            Right(user)
          }
          case None => {
            githubUserWithToken.emails.headOption flatMap { email =>
              UsersDao.findByEmail(email)
            } match {
              case Some(user) => {
                Right(user)
              }
              case None => {
                UsersDao.create(
                  createdBy = None,
                  form = UserForm(
                    email = githubUserWithToken.emails.headOption,
                    name = githubUserWithToken.name.map(GithubHelper.parseName(_))
                  )
                )
              }
            }
          }
        }

        userResult match {
          case Left(errors) => {
            Left(errors)
          }
          case Right(user) => {
            GithubUsersDao.upsertById(
              createdBy = None,
              form = GithubUserForm(
                userId = user.id,
                githubUserId = githubUserWithToken.githubId,
                login = githubUserWithToken.login
              )
            )

            TokensDao.setLatestByTag(
              createdBy = user,
              form = InternalTokenForm.GithubOauth(
                userId = user.id,
                token = githubUserWithToken.token
              )
            )

            Right(user)
          }
        }
      }
    }
  }

  /**
    * Fetches github user from an oauth code
    */
  def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUserData]]

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

  override def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUserData]] = {
    oauthClient.accessTokens.postLoginAndOauthAndAccessToken(
      AccessTokenForm(
        clientId = clientId,
        clientSecret = clientSecret,
        code = code
      )
    ).flatMap { response =>
      val client = apiClient(response.accessToken)
      for {
        githubUser <- client.users.getUser()
        emails <- client.userEmails.getUserAndEmails()
      } yield {
        // put primary first
        val sortedEmailAddresses = (emails.filter(_.primary) ++ emails.filter(!_.primary)).map(_.email)
        println(s"sortedEmailAddresses: "+ sortedEmailAddresses.mkString(" "))

        Right(
          GithubUserData(
            githubId = githubUser.id,
            login = githubUser.login,
            token = response.accessToken,
            emails = sortedEmailAddresses,
            name = githubUser.name,
            avatarUrl = githubUser.avatarUrl
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
              name = repo.name,
              visibility = if (repo.`private`) { Visibility.Private } else { Visibility.Public },
              uri = repo.htmlUrl
            )
          }
        }
      }
    }
  }

  override def oauthToken(user: User): Option[String] = {
    TokensDao.getCleartextGithubOauthTokenByUserId(user.id)
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

  override def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUserData]] = {
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

  private[this] var githubUserByCodes = scala.collection.mutable.Map[String, GithubUserData]()
  private[this] var userTokens = scala.collection.mutable.Map[String, String]()
  private[this] var repositories = scala.collection.mutable.Map[String, Repository]()
  private[this] var files = scala.collection.mutable.Map[String, String]()

  def addUser(githubUser: GithubUser, code: String, token: Option[String] = None) {
    githubUserByCodes +== (
      code -> GithubUserData(
        githubId = githubUser.id,
        login = githubUser.login,
        token = token.getOrElse(IdGenerator("tok").randomId),
        emails = Seq(githubUser.email).flatten,
        name = githubUser.name,
        avatarUrl = githubUser.avatarUrl
      )
    )
  }

  def getUserByCode(code: String): Option[GithubUserData] = {
    githubUserByCodes.lift(code)
  }

  def addUserOauthToken(token: String, user: User) {
    userTokens +== (user.id -> token)
  }

  def getToken(user: User): Option[String] = {
    userTokens.lift(user.id)
  }

  def addRepository(user: User, repository: Repository) = {
    repositories +== (user.id -> repository)
  }

  def repositories(user: User): Seq[Repository] = {
    repositories.lift(user.id) match {
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

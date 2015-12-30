package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class TokensDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "setLatestByTag" in {
    val form = InternalTokenForm.UserCreated(createTokenForm())
    val token1 = TokensDao.create(systemUser, form)

    val token2 = TokensDao.setLatestByTag(systemUser, form)
    token1.id must not be(token2.id)
  }

  "findById" in {
    val token = createToken()
    TokensDao.findById(token.id).map(_.id) must be(
      Some(token.id)
    )

    TokensDao.findById(UUID.randomUUID.toString) must be(None)
  }

  "getCleartextGithubOauthTokenByUserId" in {
    val user = createUser()
    val actualToken = "foo"
    val form = InternalTokenForm.GithubOauth(user.id, actualToken)
    val token = TokensDao.create(systemUser, form)

    TokensDao.getCleartextGithubOauthTokenByUserId(user.id) must be(Some(actualToken))
    TokensDao.getCleartextGithubOauthTokenByUserId(createUser().id) must be(None)
  }

  "incrementNumberViews" in {
    val token = createToken()

    token.cleartext.getOrElse {
      sys.error("New token must show cleartext")
    }

    TokensDao.incrementNumberViews(systemUser, token.id)

    val updated = TokensDao.findById(token.id).getOrElse {
      sys.error("Failed to fetch token")
    }
    updated.cleartext must be(None)
  }

  "findAll by ids" in {
    val token1 = createToken()
    val token2 = createToken()

    TokensDao.findAll(ids = Some(Seq(token1.id, token2.id))).map(_.id) must be(
      Seq(token1.id, token2.id)
    )

    TokensDao.findAll(ids = Some(Nil)) must be(Nil)
    TokensDao.findAll(ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
    TokensDao.findAll(ids = Some(Seq(token1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(token1.id))
  }

}

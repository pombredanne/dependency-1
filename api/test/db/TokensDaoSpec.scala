package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class TokensDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "upsert" in {
    val form = createTokenForm()
    val token1 = TokensDao.create(systemUser, form)

    val token2 = TokensDao.upsert(systemUser, form)
    token1.id must be(token2.id)

    val newToken = UUID.randomUUID.toString
    val token3 = TokensDao.upsert(systemUser, form.copy(token = newToken))

    token2.id must not be(token3.id)
    token2.token must be(form.token)
    token3.token must be(newToken)
  }

  "findById" in {
    val token = createToken()
    TokensDao.findById(token.id).map(_.id) must be(
      Some(token.id)
    )

    TokensDao.findById(UUID.randomUUID) must be(None)
  }

  "findByTokenIdAndTag" in {
    val token = createToken()
    TokensDao.findByUserIdAndTag(token.user.id, token.tag).map(_.id) must be(
      Some(token.id)
    )

    TokensDao.findByUserIdAndTag(UUID.randomUUID, token.tag).map(_.id) must be(None)
    TokensDao.findByUserIdAndTag(token.user.id, UUID.randomUUID.toString).map(_.id) must be(None)
  }

  "findAll by ids" in {
    val token1 = createToken()
    val token2 = createToken()

    TokensDao.findAll(ids = Some(Seq(token1.id, token2.id))).map(_.id) must be(
      Seq(token1.id, token2.id)
    )

    TokensDao.findAll(ids = Some(Nil)) must be(Nil)
    TokensDao.findAll(ids = Some(Seq(UUID.randomUUID))) must be(Nil)
    TokensDao.findAll(ids = Some(Seq(token1.id, UUID.randomUUID))).map(_.id) must be(Seq(token1.id))
  }

}

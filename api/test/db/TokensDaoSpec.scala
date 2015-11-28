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
    token1.guid must be(token2.guid)

    val token3 = TokensDao.upsert(systemUser, createTokenForm())

    token2.guid must not be(token3.guid)
    token2.guid must not be(token3.guid)
  }

  "findByGuid" in {
    val token = createToken()
    TokensDao.findByGuid(token.guid).map(_.guid) must be(
      Some(token.guid)
    )

    TokensDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findByTokenGuidAndTagAndToken" in {
    val token = createToken()
    TokensDao.findByUserGuidAndTagAndToken(token.user.guid, token.tag, token.token).map(_.guid) must be(
      Some(token.guid)
    )

    TokensDao.findByUserGuidAndTagAndToken(UUID.randomUUID, token.tag, token.token).map(_.guid) must be(None)
    TokensDao.findByUserGuidAndTagAndToken(token.user.guid, UUID.randomUUID.toString, token.token).map(_.guid) must be(None)
    TokensDao.findByUserGuidAndTagAndToken(token.user.guid, token.tag, UUID.randomUUID.toString).map(_.guid) must be(None)
  }

  "findAll by guids" in {
    val token1 = createToken()
    val token2 = createToken()

    TokensDao.findAll(guids = Some(Seq(token1.guid, token2.guid))).map(_.guid) must be(
      Seq(token1.guid, token2.guid)
    )

    TokensDao.findAll(guids = Some(Nil)) must be(Nil)
    TokensDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    TokensDao.findAll(guids = Some(Seq(token1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(token1.guid))
  }

}

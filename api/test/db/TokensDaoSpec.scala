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

    val newToken = UUID.randomUUID.toString
    val token3 = TokensDao.upsert(systemUser, form.copy(token = newToken))

    token2.guid must not be(token3.guid)
    token2.token must be(form.token)
    token3.token must be(newToken)
  }

  "findByGuid" in {
    val token = createToken()
    TokensDao.findByGuid(token.guid).map(_.guid) must be(
      Some(token.guid)
    )

    TokensDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findByTokenGuidAndTag" in {
    val token = createToken()
    TokensDao.findByUserGuidAndTag(token.user.id, token.tag).map(_.guid) must be(
      Some(token.guid)
    )

    TokensDao.findByUserGuidAndTag(UUID.randomUUID, token.tag).map(_.guid) must be(None)
    TokensDao.findByUserGuidAndTag(token.user.id, UUID.randomUUID.toString).map(_.guid) must be(None)
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

package com.bryzek.dependency.api.lib

import db.{Authorization, TokensDao, UsersDao}
import io.flow.common.v0.models.UserReference
import io.flow.token.v0.interfaces.Client
import io.flow.token.v0.errors.UnitResponse
import io.flow.token.v0.models.{ Token => FlowToken }
import scala.concurrent.{ExecutionContext, Future}

@javax.inject.Singleton
class DefaultTokenClient() extends Client {

  def baseUrl = throw new UnsupportedOperationException()

  def tokens: io.flow.token.v0.Tokens = new Tokens()

}

class Tokens() extends io.flow.token.v0.Tokens {

  override def get(
    tokens: Seq[String],
    requestHeaders: Seq[(String, String)] = Nil
  )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.token.v0.models.Token]] = Future {
    tokens.flatMap { t =>
      TokensDao.findByToken(t) match {
        case Some(t) => {
          Some(FlowToken(user = t.user))
        }

        case None => {
          UsersDao.findById(t).map { u => FlowToken(user = UserReference(id = u.id)) }
        }
      }
    }
  }

  override def getByToken(
    token: String,
    requestHeaders: Seq[(String, String)] = Nil
  )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.token.v0.models.Token] = {
    get(Seq(token)).map(_.headOption.getOrElse {
      throw new UnitResponse(404)
    })
  }

  override def post(
    tokenForm: io.flow.token.v0.models.TokenForm,
    requestHeaders: Seq[(String, String)] = Nil
  )(implicit ec: scala.concurrent.ExecutionContext) = throw new UnsupportedOperationException()

  override def deleteByToken(
    token: String,
    requestHeaders: Seq[(String, String)] = Nil
  )(implicit ec: scala.concurrent.ExecutionContext) = throw new UnsupportedOperationException()

}

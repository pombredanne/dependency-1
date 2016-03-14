package com.bryzek.dependency.www.lib

import io.flow.common.v0.models.UserReference
import io.flow.token.v0.interfaces.Client
import io.flow.token.v0.errors.UnitResponse
import io.flow.token.v0.models.{ Token => FlowToken }

import scala.concurrent.{ExecutionContext, Future}

@javax.inject.Singleton
class DefaultTokenClient() extends Client {

  def baseUrl = throw new UnsupportedOperationException()

  def healthchecks: io.flow.token.v0.Healthchecks = throw new UnsupportedOperationException()

  def tokens: io.flow.token.v0.Tokens = new Tokens()

}

// TODO: Add JWT for auth
class Tokens() extends io.flow.token.v0.Tokens {

  def get(
    tokens: Seq[String]
  )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.token.v0.models.Token]] = Future {
    tokens.map { userId =>
      FlowToken(
        user = UserReference(id = userId)
      )
    }
  }

  def getByToken(
    token: String
  )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.token.v0.models.Token] = {
    get(Seq(token)).map(_.headOption.getOrElse {
      throw new UnitResponse(404)
    })
  }

  def post(
    tokenForm: io.flow.token.v0.models.TokenForm
  )(implicit ec: scala.concurrent.ExecutionContext) = throw new UnsupportedOperationException()

  def deleteByToken(
    token: String
  )(implicit ec: scala.concurrent.ExecutionContext) = throw new UnsupportedOperationException()

}

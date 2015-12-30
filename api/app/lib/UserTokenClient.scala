package com.bryzek.dependency.api.lib

import db.UsersDao
import io.flow.play.clients.UserTokensClient
import io.flow.user.v0.models.User
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@javax.inject.Singleton
class DefaultUserTokensClient() extends UserTokensClient {

  override def getUserByToken(
    token: String
  )(implicit ec: ExecutionContext): Future[Option[User]] = {
    // Right now the token is just the user id
    Future {
      UsersDao.findById(token)
    }
  }

}

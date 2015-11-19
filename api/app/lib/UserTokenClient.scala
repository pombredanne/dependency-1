package com.bryzek.dependency.lib

import db.UsersDao
import io.flow.play.clients.UserTokensClient
import io.flow.user.v0.models.User
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import java.util.UUID

@javax.inject.Singleton
class DefaultUserTokensClient() extends UserTokensClient {

  override def getUserByToken(
    token: String
  )(implicit ec: ExecutionContext): Future[Option[User]] = {
    Future {
      Try(UUID.fromString(token)) match {
        case Success(guid) => {
          UsersDao.findByGuid(guid)
        }
        case Failure(_) => {
          None
        }
      }
    }
  }

}

package com.bryzek.dependency.api.lib

import db.{UserIdentifiersDao, UsersDao}
import io.flow.user.v0.models.{Name, User}
import java.util.UUID

/**
  * Information we use to render email messages, including the links
  * to unsubscribe.
  */
case class Recipient(
  email: String,
  name: Name,
  userGuid: UUID,
  identifier: String
)

object Recipient {

  def fromUser(user: User): Option[Recipient] = {
    user.email.map { email =>
      Recipient(
        email = email,
        name = user.name,
        userGuid = user.guid,
        identifier = UserIdentifiersDao.latestForUser(UsersDao.systemUser, user).value
      )
    }
  }

}



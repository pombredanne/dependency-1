package com.bryzek.dependency.actors

import io.flow.user.v0.models.User
import db.{OrganizationsDao, UsersDao}
import play.libs.Akka
import akka.actor.Actor
import java.util.UUID
import scala.concurrent.ExecutionContext

object UserActor {

  trait Message

  object Messages {
    case class Data(guid: UUID) extends Message
    case object Created extends Message
  }

}

class UserActor extends Actor with Util {

  var dataUser: Option[User] = None

  def receive = {

    case m @ UserActor.Messages.Data(guid) => withVerboseErrorHandler(m.toString) {
      dataUser = UsersDao.findByGuid(guid)
    }

    case m @ UserActor.Messages.Created => withVerboseErrorHandler(m.toString) {
      dataUser.foreach { user =>
        OrganizationsDao.upsertForUser(user)
      }
    }

    case m: Any => logUnhandledMessage(m)
  }

}

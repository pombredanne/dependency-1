package com.bryzek.dependency.actors

import com.bryzek.dependency.lib.DefaultBinaryVersionProvider
import com.bryzek.dependency.v0.models.Binary
import io.flow.play.postgresql.Pager
import db.{BinariesDao, BinaryVersionsDao, SyncsDao, UsersDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object BinaryActor {

  object Messages {
    case class Data(guid: UUID)
    case object Sync
  }

}

class BinaryActor extends Actor {

  var dataBinary: Option[Binary] = None

  def receive = {

    case m @ BinaryActor.Messages.Data(guid: UUID) => Util.withVerboseErrorHandler(m) {
      dataBinary = BinariesDao.findByGuid(guid)
    }

    case m @ BinaryActor.Messages.Sync => Util.withVerboseErrorHandler(m) {
      dataBinary.foreach { binary =>
        SyncsDao.withStartedAndCompleted(MainActor.SystemUser, binary.guid) {
          DefaultBinaryVersionProvider.versions(binary.name).foreach { version =>
            BinaryVersionsDao.upsert(UsersDao.systemUser, binary.guid, version.value)
          }
        }

        // TODO: Should we only send if something changed?
        sender ! MainActor.Messages.BinarySyncCompleted(binary.guid)
      }
    }

    case m: Any => {
      Logger.error("Binary actor got an unhandled mesage: " + m)
    }
  }

}

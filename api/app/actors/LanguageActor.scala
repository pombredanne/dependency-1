package com.bryzek.dependency.actors

import com.bryzek.dependency.lib.DefaultBinaryVersionProvider
import com.bryzek.dependency.v0.models.Binary
import io.flow.play.postgresql.Pager
import db.{BinariesDao, BinaryVersionsDao, UsersDao}
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

    case BinaryActor.Messages.Data(guid: UUID) => Util.withVerboseErrorHandler(
      s"BinaryActor.Messages.Data($guid)"
    ) {
      dataBinary = BinariesDao.findByGuid(guid)
      self ! BinaryActor.Messages.Sync
    }

    case BinaryActor.Messages.Sync => Util.withVerboseErrorHandler(
      s"BinaryActor.Messages.Sync"
    ) {
      dataBinary.foreach { lang =>
        DefaultBinaryVersionProvider.versions(lang.name).foreach { version =>
          // TODO: fetch all versions for this binary and store them
          println(s"Store version[${version.value}] from lang[$lang]")
          BinaryVersionsDao.upsert(UsersDao.systemUser, lang.guid, version.value)
        }
      }
    }

    case m: Any => {
      Logger.error("Binary actor got an unhandled mesage: " + m)
    }
  }

}

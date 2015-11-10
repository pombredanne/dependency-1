package com.bryzek.dependency.actors

import com.bryzek.dependency.v0.models.Library
import io.flow.play.postgresql.Pager
import db.{LibrariesDao, LibraryVersionsDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object LibraryActor {

  object Messages {
    case class Data(guid: UUID)
    case object Sync
  }

}

class LibraryActor extends Actor {

  var dataLibrary: Option[Library] = None

  def receive = {

    case LibraryActor.Messages.Data(guid: UUID) => Util.withVerboseErrorHandler(
      s"LibraryActor.Messages.Data($guid)"
    ) {
      dataLibrary = LibrariesDao.findByGuid(guid)
      self ! LibraryActor.Messages.Sync
    }

    case LibraryActor.Messages.Sync => Util.withVerboseErrorHandler(
      s"LibraryActor.Messages.Sync"
    ) {
      dataLibrary.foreach { lib =>
        // TODO: fetch all versions for this library and store them
      }
    }

    case m: Any => {
      Logger.error("Library actor got an unhandled mesage: " + m)
    }
  }

}

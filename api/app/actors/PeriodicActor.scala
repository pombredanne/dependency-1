package com.bryzek.dependency.actors

import io.flow.play.postgresql.Pager
import db.{BinariesDao, LibrariesDao, ProjectsDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object PeriodicActor {

  sealed trait Message

  object Messages {
    case object SyncBinaries extends Message
    case object SyncLibraries extends Message
    case object SyncProjects extends Message
  }

}

class PeriodicActor extends Actor {

  def receive = {

    case PeriodicActor.Messages.SyncProjects => Util.withVerboseErrorHandler(
      s"PeriodicActor.Messages.SyncProjects"
    ) {
      Pager.eachPage { offset =>
        ProjectsDao.findAll(offset = offset)
      } { project =>
        sender ! MainActor.Messages.ProjectSync(project.guid)
      }
    }

    case PeriodicActor.Messages.SyncBinaries => Util.withVerboseErrorHandler(
      s"PeriodicActor.Messages.SyncBinaries"
    ) {
      Pager.eachPage { offset =>
        BinariesDao.findAll(offset = offset)
      } { bin =>
        sender ! MainActor.Messages.BinarySync(bin.guid)
      }
    }

    case PeriodicActor.Messages.SyncLibraries => Util.withVerboseErrorHandler(
      s"PeriodicActor.Messages.SyncLibraries"
    ) {
      Pager.eachPage { offset =>
        LibrariesDao.findAll(offset = offset)
      } { library =>
        sender ! MainActor.Messages.LibrarySync(library.guid)
      }
    }

    case m: Any => {
      Logger.error("Periodic actor got an unhandled message: " + m)
    }
  }

}

package com.bryzek.dependency.actors

import io.flow.play.postgresql.Pager
import db.{BinariesDao, LibrariesDao, ProjectsDao, SyncsDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object PeriodicActor {

  sealed trait Message

  object Messages {
    case object SyncBinaries extends Message
    case object SyncLibraries extends Message
    case object SyncProjects extends Message
    case object Purge extends Message
  }

}

class PeriodicActor extends Actor with Util {

  def receive = {

    case m @ PeriodicActor.Messages.SyncProjects => withVerboseErrorHandler(m) {
      Pager.eachPage { offset =>
        ProjectsDao.findAll(offset = offset)
      } { project =>
        sender ! MainActor.Messages.ProjectSync(project.guid)
      }
    }

    case m @ PeriodicActor.Messages.SyncBinaries => withVerboseErrorHandler(m) {
      Pager.eachPage { offset =>
        BinariesDao.findAll(offset = offset)
      } { bin =>
        sender ! MainActor.Messages.BinarySync(bin.guid)
      }
    }

    case m @ PeriodicActor.Messages.SyncLibraries => withVerboseErrorHandler(m) {
      Pager.eachPage { offset =>
        LibrariesDao.findAll(offset = offset)
      } { library =>
        sender ! MainActor.Messages.LibrarySync(library.guid)
      }
    }

    case m @ PeriodicActor.Messages.Purge => withVerboseErrorHandler(m) {
      SyncsDao.purgeOld()
    }

    case m: Any => {
      Logger.error("Periodic actor got an unhandled message: " + m)
    }
  }

}

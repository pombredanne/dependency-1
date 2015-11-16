package com.bryzek.dependency.actors

import io.flow.play.postgresql.Pager
import db.{LibrariesDao, ProjectsDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object PeriodicActor {

  object Messages {
    case object SyncLibraries
    case object SyncProjects
  }

}

@javax.inject.Singleton
class PeriodicActor @javax.inject.Inject() (
  librariesDao: LibrariesDao,
  projectsDao: ProjectsDao
) extends Actor {

  def receive = {

    case PeriodicActor.Messages.SyncProjects => Util.withVerboseErrorHandler(
      s"PeriodicActor.Messages.SyncProjects"
    ) {
      Pager.eachPage { offset =>
        projectsDao.findAll(offset = offset)
      } { project =>
        sender ! MainActor.Messages.ProjectSync(project.guid)
      }
    }

    case PeriodicActor.Messages.SyncLibraries => Util.withVerboseErrorHandler(
      s"PeriodicActor.Messages.SyncLibraries"
    ) {
      Pager.eachPage { offset =>
        librariesDao.findAll(offset = offset)
      } { library =>
        sender ! MainActor.Messages.LibrarySync(library.guid)
      }
    }

    case m: Any => {
      Logger.error("Periodic actor got an unhandled message: " + m)
    }
  }

}

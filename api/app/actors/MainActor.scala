package com.bryzek.dependency.actors

import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor._
import play.api.Logger
import play.api.Play.current
import java.util.UUID

object MainActor {

  def props() = Props(new MainActor("main"))

  lazy val ref = Akka.system.actorOf(props(), "main")

  lazy val SystemUser = db.UsersDao.systemUser

  object Messages {

    case class ProjectCreated(guid: UUID)
    case class ProjectDeleted(guid: UUID)

    case class LibraryCreated(guid: UUID)
    case class LibraryDeleted(guid: UUID)

    case class LanguageCreated(guid: UUID)
    case class LanguageDeleted(guid: UUID)

  }
}


class MainActor(name: String) extends Actor with ActorLogging {
  import scala.concurrent.duration._

  private[this] val projectActor = Akka.system.actorOf(Props[ProjectActor], name = s"$name:projectActor")

  Akka.system.scheduler.schedule(12.hours, 1.day, projectActor, ProjectActor.Messages.SyncAll)

  def receive = akka.event.LoggingReceive {

    case MainActor.Messages.ProjectCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.ProjectCreated($guid)"
    ) {
      projectActor ! ProjectActor.Messages.Sync(guid)
    }

    case MainActor.Messages.ProjectDeleted(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.ProjectDeleted($guid)"
    ) {
      // NO-OP
    }

    case MainActor.Messages.LibraryCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.LibraryCreated($guid)"
    ) {
      // TODO
    }

    case MainActor.Messages.LibraryDeleted(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.LibraryDeleted($guid)"
    ) {
      // TODO
    }

    case MainActor.Messages.LanguageCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.LanguageCreated($guid)"
    ) {
      // TODO
    }

    case MainActor.Messages.LanguageDeleted(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.LanguageDeleted($guid)"
    ) {
      // TODO
    }

    case m: Any => {
      Logger.error("Main actor got an unhandled message: " + m)
    }

  }

}

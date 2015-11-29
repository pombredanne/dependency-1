package com.bryzek.dependency.actors

import play.api.libs.concurrent.Akka
import akka.actor._
import play.api.Logger
import play.api.Play.current
import java.util.UUID
import scala.concurrent.ExecutionContext

object MainActor {

  def props() = Props(new MainActor("main"))

  lazy val ref = Akka.system.actorOf(props(), "main")

  lazy val SystemUser = db.UsersDao.systemUser

  object Messages {

    case class ProjectCreated(guid: UUID)
    case class ProjectUpdated(guid: UUID)
    case class ProjectDeleted(guid: UUID)
    case class ProjectSync(guid: UUID)

    case class LibraryCreated(guid: UUID)
    case class LibraryDeleted(guid: UUID)
    case class LibrarySync(guid: UUID)

    case class LanguageCreated(guid: UUID)
    case class LanguageDeleted(guid: UUID)
    case class LanguageSync(guid: UUID)

  }
}


class MainActor(name: String) extends Actor with ActorLogging {
  import scala.concurrent.duration._

  private[this] val periodicActor = Akka.system.actorOf(Props[PeriodicActor], name = s"$name:periodicActor")

  private[this] val projectActors = scala.collection.mutable.Map[UUID, ActorRef]()
  private[this] val libraryActors = scala.collection.mutable.Map[UUID, ActorRef]()
  private[this] val languageActors = scala.collection.mutable.Map[UUID, ActorRef]()

  implicit val mainActorExecutionContext: ExecutionContext = Akka.system.dispatchers.lookup("main-actor-context")

  Akka.system.scheduler.schedule(15.minutes, 1.day, periodicActor, PeriodicActor.Messages.SyncProjects)
  Akka.system.scheduler.schedule(15.minutes, 1.day, periodicActor, PeriodicActor.Messages.SyncLibraries)

  def receive = akka.event.LoggingReceive {

    case MainActor.Messages.ProjectCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.ProjectCreated($guid)"
    ) {
      upsertProjectActor(guid) ! ProjectActor.Messages.Sync
    }

    case MainActor.Messages.ProjectUpdated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.ProjectUpdated($guid)"
    ) {
      upsertProjectActor(guid) ! ProjectActor.Messages.Sync
    }

    case MainActor.Messages.ProjectDeleted(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.ProjectDeleted($guid)"
    ) {
      projectActors -= guid
    }

    case MainActor.Messages.ProjectSync(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.ProjectSync($guid)"
    ) {
      upsertProjectActor(guid) ! ProjectActor.Messages.Sync
    }

    case MainActor.Messages.LibraryCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.LibraryCreated($guid)"
    ) {
      upsertLibraryActor(guid) ! LibraryActor.Messages.Sync
    }

    case MainActor.Messages.LibrarySync(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.LibrarySync($guid)"
    ) {
      libraryActors -= guid
    }

    case MainActor.Messages.LibraryDeleted(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.LibraryDeleted($guid)"
    ) {
      upsertLibraryActor(guid) ! LibraryActor.Messages.Sync
    }

    case MainActor.Messages.LanguageCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.LanguageCreated($guid)"
    ) {
      upsertLanguageActor(guid) ! LanguageActor.Messages.Sync
    }

    case MainActor.Messages.LanguageSync(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.LanguageSync($guid)"
    ) {
      languageActors -= guid
    }

    case MainActor.Messages.LanguageDeleted(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.LanguageDeleted($guid)"
    ) {
      upsertLanguageActor(guid) ! LanguageActor.Messages.Sync
    }

    case m: Any => {
      Logger.error("Main actor got an unhandled message: " + m)
    }

  }

  def upsertProjectActor(guid: UUID): ActorRef = {
    projectActors.lift(guid).getOrElse {
      val ref = Akka.system.actorOf(Props[ProjectActor], name = s"$name:projectActor:$guid")
      ref ! ProjectActor.Messages.Data(guid)
      projectActors += (guid -> ref)
      ref
    }
  }

  def upsertLibraryActor(guid: UUID): ActorRef = {
    libraryActors.lift(guid).getOrElse {
      val ref = Akka.system.actorOf(Props[LibraryActor], name = s"$name:libraryActor:$guid")
      ref ! LibraryActor.Messages.Data(guid)
      libraryActors += (guid -> ref)
      ref
    }
  }

  def upsertLanguageActor(guid: UUID): ActorRef = {
    languageActors.lift(guid).getOrElse {
      val ref = Akka.system.actorOf(Props[LanguageActor], name = s"$name:languageActor:$guid")
      ref ! LanguageActor.Messages.Data(guid)
      languageActors += (guid -> ref)
      ref
    }
  }

}

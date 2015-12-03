package com.bryzek.dependency.actors

import io.flow.play.util.DefaultConfig
import play.api.libs.concurrent.Akka
import akka.actor._
import play.api.Logger
import play.api.Play.current
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

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

    case class BinaryCreated(guid: UUID)
    case class BinaryDeleted(guid: UUID)
    case class BinarySync(guid: UUID)

  }
}


class MainActor(name: String) extends Actor with ActorLogging {
  import scala.concurrent.duration._

  private[this] val periodicActor = Akka.system.actorOf(Props[PeriodicActor], name = s"$name:periodicActor")

  private[this] val projectActors = scala.collection.mutable.Map[UUID, ActorRef]()
  private[this] val libraryActors = scala.collection.mutable.Map[UUID, ActorRef]()
  private[this] val binaryActors = scala.collection.mutable.Map[UUID, ActorRef]()

  implicit val mainActorExecutionContext: ExecutionContext = Akka.system.dispatchers.lookup("main-actor-context")

  /**
   * Helper to schedule a message to be sent on a recurring interval
   * based on a configuration parameter.
   *
   * @param configName The name of the configuration parameter containing the number
   *        of seconds between runs. You can also optionally add a configuration
   *        parameter of the same name with "_inital" appended to set the initial
   *        interval if you wish it to be different.
   */
  private[this] def scheduleRecurring(configName: String, message: PeriodicActor.Message) {
    val seconds = DefaultConfig.requiredString(configName).toInt
    val initial = DefaultConfig.optionalString(s"${configName}_initial").map(_.toInt).getOrElse(seconds)
    println(s"scheduling a periodic message[$message]. Initial[$initial seconds], recurring[$seconds seconds]")
    Akka.system.scheduler.schedule(
      FiniteDuration(initial, SECONDS),
      FiniteDuration(seconds, SECONDS),
      periodicActor, message
    )
  }

  scheduleRecurring("com.bryzek.dependency.project.sync.seconds", PeriodicActor.Messages.SyncProjects)
  scheduleRecurring("com.bryzek.dependency.library.sync.seconds", PeriodicActor.Messages.SyncLibraries)
  def receive = akka.event.LoggingReceive {

    case MainActor.Messages.ProjectCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.ProjectCreated($guid)"
    ) {
      upsertProjectActor(guid) ! ProjectActor.Messages.Watch
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

    case MainActor.Messages.BinaryCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.BinaryCreated($guid)"
    ) {
      upsertBinaryActor(guid) ! BinaryActor.Messages.Sync
    }

    case MainActor.Messages.BinarySync(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.BinarySync($guid)"
    ) {
      binaryActors -= guid
    }

    case MainActor.Messages.BinaryDeleted(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.BinaryDeleted($guid)"
    ) {
      upsertBinaryActor(guid) ! BinaryActor.Messages.Sync
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

  def upsertBinaryActor(guid: UUID): ActorRef = {
    binaryActors.lift(guid).getOrElse {
      val ref = Akka.system.actorOf(Props[BinaryActor], name = s"$name:binaryActor:$guid")
      ref ! BinaryActor.Messages.Data(guid)
      binaryActors += (guid -> ref)
      ref
    }
  }

}

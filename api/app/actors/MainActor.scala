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

    case class SyncCreateProjectWatch(objectGuid: UUID)
    case class SyncDeleteProjectWatch(objectGuid: UUID)
  }
}


class MainActor(name: String) extends Actor with ActorLogging {
  import scala.concurrent.duration._

  private[this] val periodicActor = Akka.system.actorOf(Props[PeriodicActor], name = s"$name:periodicActor")
  private[this] val searchActor = Akka.system.actorOf(Props[SearchActor], name = s"$name:SearchActor")
  private[this] val syncActor = Akka.system.actorOf(Props[SyncActor], name = s"$name:SyncActor")

  private[this] val binaryActors = scala.collection.mutable.Map[UUID, ActorRef]()
  private[this] val libraryActors = scala.collection.mutable.Map[UUID, ActorRef]()
  private[this] val projectActors = scala.collection.mutable.Map[UUID, ActorRef]()

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
    Logger.info(s"scheduling a periodic message[$message]. Initial[$initial seconds], recurring[$seconds seconds]")
    Akka.system.scheduler.schedule(
      FiniteDuration(initial, SECONDS),
      FiniteDuration(seconds, SECONDS),
      periodicActor, message
    )
  }

  scheduleRecurring("com.bryzek.dependency.binary.sync.seconds", PeriodicActor.Messages.SyncBinaries)
  scheduleRecurring("com.bryzek.dependency.library.sync.seconds", PeriodicActor.Messages.SyncLibraries)
  scheduleRecurring("com.bryzek.dependency.project.sync.seconds", PeriodicActor.Messages.SyncProjects)
  scheduleRecurring("com.bryzek.dependency.purge.seconds", PeriodicActor.Messages.Purge)

  def receive = akka.event.LoggingReceive {

    case m @ MainActor.Messages.ProjectCreated(guid) => Util.withVerboseErrorHandler(m) {
      val actor = upsertProjectActor(guid)
      actor ! ProjectActor.Messages.Watch
      actor ! ProjectActor.Messages.Sync
      searchActor ! SearchActor.Messages.SyncProject(guid)
    }

    case m @ MainActor.Messages.ProjectUpdated(guid) => Util.withVerboseErrorHandler(m) {
      upsertProjectActor(guid) ! ProjectActor.Messages.Sync
      searchActor ! SearchActor.Messages.SyncProject(guid)
    }

    case m @ MainActor.Messages.ProjectDeleted(guid) => Util.withVerboseErrorHandler(m) {
      projectActors -= guid
      searchActor ! SearchActor.Messages.SyncProject(guid)
    }

    case m @ MainActor.Messages.ProjectSync(guid) => Util.withVerboseErrorHandler(m) {
      upsertProjectActor(guid) ! ProjectActor.Messages.Sync
    }

    case m @ MainActor.Messages.LibraryCreated(guid) => Util.withVerboseErrorHandler(m) {
      upsertLibraryActor(guid) ! LibraryActor.Messages.Sync
      searchActor ! SearchActor.Messages.SyncLibrary(guid)
      syncActor ! SyncActor.Messages.Broadcast
    }

    case m @ MainActor.Messages.LibrarySync(guid) => Util.withVerboseErrorHandler(m) {
      libraryActors -= guid
    }

    case m @ MainActor.Messages.LibraryDeleted(guid) => Util.withVerboseErrorHandler(m) {
      upsertLibraryActor(guid) ! LibraryActor.Messages.Sync
      searchActor ! SearchActor.Messages.SyncLibrary(guid)
    }

    case m @ MainActor.Messages.BinaryCreated(guid) => Util.withVerboseErrorHandler(m) {
      upsertBinaryActor(guid) ! BinaryActor.Messages.Sync
      searchActor ! SearchActor.Messages.SyncBinary(guid)
      syncActor ! SyncActor.Messages.Broadcast
    }

    case m @ MainActor.Messages.BinarySync(guid) => Util.withVerboseErrorHandler(m) {
      binaryActors -= guid
    }

    case m @ MainActor.Messages.BinaryDeleted(guid) => Util.withVerboseErrorHandler(m) {
      upsertBinaryActor(guid) ! BinaryActor.Messages.Sync
      searchActor ! SearchActor.Messages.SyncBinary(guid)
    }

    case m @ MainActor.Messages.SyncCreateProjectWatch(objectGuid) => Util.withVerboseErrorHandler(m) {
      syncActor ! SyncActor.Messages.CreateProjectWatch(objectGuid)
    }

    case m @ MainActor.Messages.SyncDeleteProjectWatch(objectGuid) => Util.withVerboseErrorHandler(m) {
      syncActor ! SyncActor.Messages.DeleteProjectWatch(objectGuid)
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

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
    case class LibraryUpdated(guid: UUID)
    case class LibraryDeleted(guid: UUID)
    case class LibrarySync(guid: UUID)
    case class LibrarySyncCompleted(guid: UUID)

    case class BinaryCreated(guid: UUID)
    case class BinaryDeleted(guid: UUID)
    case class BinarySync(guid: UUID)
    case class BinarySyncCompleted(guid: UUID)
  }
}


class MainActor(name: String) extends Actor with ActorLogging with Util {
  import scala.concurrent.duration._

  private[this] val periodicActor = Akka.system.actorOf(Props[PeriodicActor], name = s"$name:periodicActor")
  private[this] val searchActor = Akka.system.actorOf(Props[SearchActor], name = s"$name:SearchActor")

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

    case m @ MainActor.Messages.ProjectCreated(guid) => withVerboseErrorHandler(m) {
      val actor = upsertProjectActor(guid)
      actor ! ProjectActor.Messages.Watch
      actor ! ProjectActor.Messages.Sync
      searchActor ! SearchActor.Messages.SyncProject(guid)
    }

    case m @ MainActor.Messages.ProjectUpdated(guid) => withVerboseErrorHandler(m) {
      upsertProjectActor(guid) ! ProjectActor.Messages.Sync
      searchActor ! SearchActor.Messages.SyncProject(guid)
    }

    case m @ MainActor.Messages.ProjectDeleted(guid) => withVerboseErrorHandler(m) {
      // TODO: Cleanup recommendations for this project
      projectActors.remove(guid).map { context.stop(_) }
      searchActor ! SearchActor.Messages.SyncProject(guid)
    }

    case m @ MainActor.Messages.ProjectSync(guid) => withVerboseErrorHandler(m) {
      upsertProjectActor(guid) ! ProjectActor.Messages.Sync
    }

    case m @ MainActor.Messages.LibraryCreated(guid) => withVerboseErrorHandler(m) {
      syncLibrary(guid)
    }

    case m @ MainActor.Messages.LibraryUpdated(guid) => withVerboseErrorHandler(m) {
      // No-op
    }

    case m @ MainActor.Messages.LibrarySync(guid) => withVerboseErrorHandler(m) {
      syncLibrary(guid)
    }

    case m @ MainActor.Messages.LibrarySyncCompleted(guid) => withVerboseErrorHandler(m) {
      projectBroadcast(ProjectActor.Messages.LibrarySynced(guid))
    }

    case m @ MainActor.Messages.LibraryDeleted(guid) => withVerboseErrorHandler(m) {
      libraryActors.remove(guid).map { context.stop(_) }
    }

    case m @ MainActor.Messages.BinaryCreated(guid) => withVerboseErrorHandler(m) {
      syncBinary(guid)
    }

    case m @ MainActor.Messages.BinarySync(guid) => withVerboseErrorHandler(m) {
      syncBinary(guid)
    }

    case m @ MainActor.Messages.BinarySyncCompleted(guid) => withVerboseErrorHandler(m) {
      projectBroadcast(ProjectActor.Messages.BinarySynced(guid))
    }

    case m @ MainActor.Messages.BinaryDeleted(guid) => withVerboseErrorHandler(m) {
      binaryActors.remove(guid).map { context.stop(_) }
    }

    case m: Any => logUnhandledMessage(m)

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

  def syncLibrary(guid: UUID) {
    upsertLibraryActor(guid) ! LibraryActor.Messages.Sync
    searchActor ! SearchActor.Messages.SyncLibrary(guid)
  }

  def syncBinary(guid: UUID) {
    upsertBinaryActor(guid) ! BinaryActor.Messages.Sync
    searchActor ! SearchActor.Messages.SyncBinary(guid)
  }

  def projectBroadcast(message: ProjectActor.Message) {
    projectActors.foreach { case (projectGuid, actor) =>
      actor ! message
    }
  }

}

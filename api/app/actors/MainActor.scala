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

    case class ProjectLibraryCreated(projectGuid: UUID, guid: UUID)
    case class ProjectLibrarySync(projectGuid: UUID, guid: UUID)
    case class ProjectLibraryDeleted(projectGuid: UUID, guid: UUID)

    case class ProjectBinaryCreated(projectGuid: UUID, guid: UUID)
    case class ProjectBinarySync(projectGuid: UUID, guid: UUID)
    case class ProjectBinaryDeleted(projectGuid: UUID, guid: UUID)

    case class ResolverCreated(guid: UUID)
    case class ResolverDeleted(guid: UUID)

    case class LibraryCreated(guid: UUID)
    case class LibraryDeleted(guid: UUID)
    case class LibrarySync(guid: UUID)
    case class LibrarySyncCompleted(guid: UUID)

    case class BinaryCreated(guid: UUID)
    case class BinaryDeleted(guid: UUID)
    case class BinarySync(guid: UUID)
    case class BinarySyncCompleted(guid: UUID)

    case class UserCreated(guid: UUID)
  }
}


class MainActor(name: String) extends Actor with ActorLogging with Util {
  import scala.concurrent.duration._

  private[this] val emailActor = Akka.system.actorOf(Props[EmailActor], name = s"$name:emailActor")
  private[this] val periodicActor = Akka.system.actorOf(Props[PeriodicActor], name = s"$name:periodicActor")
  private[this] val searchActor = Akka.system.actorOf(Props[SearchActor], name = s"$name:SearchActor")

  private[this] val binaryActors = scala.collection.mutable.Map[UUID, ActorRef]()
  private[this] val libraryActors = scala.collection.mutable.Map[UUID, ActorRef]()
  private[this] val projectActors = scala.collection.mutable.Map[UUID, ActorRef]()
  private[this] val userActors = scala.collection.mutable.Map[UUID, ActorRef]()
  private[this] val resolverActors = scala.collection.mutable.Map[UUID, ActorRef]()

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
  private[this] def scheduleRecurring[T](
    actor: ActorRef,
    configName: String,
    message: T
  ) {
    val seconds = DefaultConfig.requiredString(configName).toInt
    val initial = DefaultConfig.optionalString(s"${configName}_initial").map(_.toInt).getOrElse(seconds)
    Logger.info(s"scheduling a periodic message[$message]. Initial[$initial seconds], recurring[$seconds seconds]")
    Akka.system.scheduler.schedule(
      FiniteDuration(initial, SECONDS),
      FiniteDuration(seconds, SECONDS),
      actor, message
    )
  }

  scheduleRecurring(periodicActor, "com.bryzek.dependency.api.binary.seconds", PeriodicActor.Messages.SyncBinaries)
  scheduleRecurring(periodicActor, "com.bryzek.dependency.api.library.seconds", PeriodicActor.Messages.SyncLibraries)
  scheduleRecurring(periodicActor, "com.bryzek.dependency.api.project.seconds", PeriodicActor.Messages.SyncProjects)
  scheduleRecurring(periodicActor, "com.bryzek.dependency.api.purge.seconds", PeriodicActor.Messages.Purge)
  scheduleRecurring(emailActor, "com.bryzek.dependency.api.email.seconds", EmailActor.Messages.ProcessDailySummary)

  def receive = akka.event.LoggingReceive {

    case m @ MainActor.Messages.UserCreated(guid) => withVerboseErrorHandler(m) {
      upsertUserActor(guid) ! UserActor.Messages.Created
    }

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
      projectActors.remove(guid).map { actor =>
        actor ! ProjectActor.Messages.Deleted
        context.stop(_)
      }
      searchActor ! SearchActor.Messages.SyncProject(guid)
    }

    case m @ MainActor.Messages.ProjectSync(guid) => withVerboseErrorHandler(m) {
      upsertProjectActor(guid) ! ProjectActor.Messages.Sync
    }

    case m @ MainActor.Messages.ProjectLibraryCreated(projectGuid, guid) => withVerboseErrorHandler(m) {
      upsertProjectActor(projectGuid) ! ProjectActor.Messages.ProjectLibraryCreated(guid)
    }

    case m @ MainActor.Messages.ProjectLibrarySync(projectGuid, guid) => withVerboseErrorHandler(m) {
      upsertProjectActor(projectGuid) ! ProjectActor.Messages.ProjectLibrarySync(guid)
    }

    case m @ MainActor.Messages.ProjectLibraryDeleted(projectGuid, guid) => withVerboseErrorHandler(m) {
      // intentional no-op
    }

    case m @ MainActor.Messages.ProjectBinaryCreated(projectGuid, guid) => withVerboseErrorHandler(m) {
      upsertProjectActor(projectGuid) ! ProjectActor.Messages.ProjectBinaryCreated(guid)
    }

    case m @ MainActor.Messages.ProjectBinarySync(projectGuid, guid) => withVerboseErrorHandler(m) {
      upsertProjectActor(projectGuid) ! ProjectActor.Messages.ProjectBinarySync(guid)
    }

    case m @ MainActor.Messages.ProjectBinaryDeleted(projectGuid, guid) => withVerboseErrorHandler(m) {
      // intentional no-op
    }

    case m @ MainActor.Messages.LibraryCreated(guid) => withVerboseErrorHandler(m) {
      syncLibrary(guid)
    }

    case m @ MainActor.Messages.LibrarySync(guid) => withVerboseErrorHandler(m) {
      syncLibrary(guid)
    }

    case m @ MainActor.Messages.LibrarySyncCompleted(guid) => withVerboseErrorHandler(m) {
      projectBroadcast(ProjectActor.Messages.LibrarySynced(guid))
    }

    case m @ MainActor.Messages.LibraryDeleted(guid) => withVerboseErrorHandler(m) {
      libraryActors.remove(guid).map { ref =>
        ref ! LibraryActor.Messages.Deleted
        context.stop(ref)
      }
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
      binaryActors.remove(guid).map { ref =>
        ref ! BinaryActor.Messages.Deleted
        context.stop(ref)
      }

    }

    case m @ MainActor.Messages.ResolverCreated(guid) => withVerboseErrorHandler(m) {
      resolverActors.remove(guid).map { ref =>
        ref ! ResolverActor.Messages.Deleted
        context.stop(ref)
      }
    }

    case m @ MainActor.Messages.ResolverDeleted(guid) => withVerboseErrorHandler(m) {
      resolverActors.remove(guid).map { context.stop(_) }
    }

    case m: Any => logUnhandledMessage(m)

  }

  def upsertUserActor(guid: UUID): ActorRef = {
    userActors.lift(guid).getOrElse {
      val ref = Akka.system.actorOf(Props[UserActor], name = s"$name:userActor:$guid")
      ref ! UserActor.Messages.Data(guid)
      userActors += (guid -> ref)
      ref
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

  def upsertResolverActor(guid: UUID): ActorRef = {
    resolverActors.lift(guid).getOrElse {
      val ref = Akka.system.actorOf(Props[ResolverActor], name = s"$name:resolverActor:$guid")
      ref ! ResolverActor.Messages.Data(guid)
      resolverActors += (guid -> ref)
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

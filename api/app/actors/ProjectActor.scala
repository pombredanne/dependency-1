package com.bryzek.dependency.actors

import com.bryzek.dependency.lib.{Dependencies, GithubDependencyProviderClient}
import com.bryzek.dependency.v0.models.{Project, WatchProjectForm}
import db.{BinariesDao, LibrariesDao, ProjectsDao, RecommendationsDao, SyncsDao, UsersDao, WatchProjectsDao}
import play.api.Logger
import play.libs.Akka
import akka.actor.Actor
import java.util.UUID
import scala.concurrent.ExecutionContext

object ProjectActor {

  trait Message

  object Messages {
    case class Data(guid: UUID) extends Message
    case object Sync extends Message
    case object SyncCompleted extends Message
    case object Watch extends Message

    case class LibrarySynced(guid: UUID) extends Message
    case class BinarySynced(guid: UUID) extends Message
  }

}

class ProjectActor extends Actor with Util {

  implicit val projectExecutionContext: ExecutionContext = Akka.system.dispatchers.lookup("project-actor-context")

  var dataProject: Option[Project] = None
  var pendingSync: Option[Boolean] = None

  def receive = {

    case m @ ProjectActor.Messages.Data(guid) => withVerboseErrorHandler(m.toString) {
      dataProject = ProjectsDao.findByGuid(guid)
    }

    case m @ ProjectActor.Messages.Watch => withVerboseErrorHandler(m.toString) {
      dataProject.foreach { project =>
        WatchProjectsDao.upsert(
          MainActor.SystemUser,
          WatchProjectForm(
            userGuid = project.audit.createdBy.guid,
            projectGuid = project.guid
          )
        )
      }
    }

    case m @ ProjectActor.Messages.LibrarySynced(guid) => withVerboseErrorHandler(m.toString) {
      dataProject.foreach { project =>
        processPendingSync(project)
      }
    }

    case m @ ProjectActor.Messages.BinarySynced(guid) => withVerboseErrorHandler(m.toString) {
      dataProject.foreach { project =>
        processPendingSync(project)
      }
    }

    case m @ ProjectActor.Messages.Sync => withVerboseErrorHandler(m.toString) {
      dataProject.foreach { project =>
        SyncsDao.recordStarted(MainActor.SystemUser, project.guid)

        UsersDao.findByGuid(project.audit.createdBy.guid).map { user =>
          GithubDependencyProviderClient.instance(user).dependencies(project).map { dependencies =>
            println(s" - project[${project.guid}] name[${project.name}] dependencies: $dependencies")
            ProjectsDao.setDependencies(
              createdBy = MainActor.SystemUser,
              project = project,
              binaries = dependencies.binaries,
              libraries = dependencies.librariesAndPlugins.map(_.map { artifact =>
                artifact.toLibraryForm(
                  crossBuildVersion = dependencies.crossBuildVersion()
                )
              })
            )
          }.recover {
            case e => {
              Logger.error(s"Error fetching dependencies for project[${project.guid}] name[${project.name}]: $e")
            }
          }

          println("Setting pendingSync to true")
          pendingSync = Some(true)
          processPendingSync(project)
        }

      }
    }

    case m: Any => logUnhandledMessage(m)
  }

  def processPendingSync(project: Project) {
    println(s"processPendingSync for project[${project.name}]")
    pendingSync.foreach { _ =>
      dependenciesPendingCompletion(project) match {
        case Nil => {
          println(s" -- all dependencies are synced.")
          RecommendationsDao.sync(MainActor.SystemUser, project)
          pendingSync.map { _ =>
            SyncsDao.recordCompleted(MainActor.SystemUser, project.guid)
            pendingSync = None
          }
        }
        case deps => {
          println(s" -- still waiting on " + deps.mkString(", "))
        }
      }
    }
  }

  // NB: We don't return ALL dependencies
  private[this] def dependenciesPendingCompletion(project: Project): Seq[String] = {
    LibrariesDao.findAll(
      projectGuid = Some(project.guid),
      isSynced = Some(false)
    ).map( lib => s"Library ${lib.groupId}.${lib.artifactId}" ) ++ 
    BinariesDao.findAll(
      projectGuid = Some(project.guid),
      isSynced = Some(false)
    ).map( bin => s"Binary ${bin.name}" )
  }

}

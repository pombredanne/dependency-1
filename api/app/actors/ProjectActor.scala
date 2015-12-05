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
        LibrariesDao.findAll(guid = Some(guid), projectGuid = Some(project.guid), limit = 1).headOption.map { _ =>
          processPendingSync(project)
        }
      }
    }

    case m @ ProjectActor.Messages.BinarySynced(guid) => withVerboseErrorHandler(m.toString) {
      dataProject.foreach { project =>
        BinariesDao.findAll(guid = Some(guid), projectGuid = Some(project.guid), limit = 1).headOption.map { _ =>
          processPendingSync(project)
        }
      }
    }

    case m @ ProjectActor.Messages.Sync => withVerboseErrorHandler(m.toString) {
      dataProject.foreach { project =>
        SyncsDao.recordStarted(MainActor.SystemUser, project.guid)
        pendingSync = Some(true)

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
          // RecommendationsDao.sync(MainActor.SystemUser, project)
          // TODO: Need to mark sync complete if all libraries / binaries are already present
        }

      }
    }

    case m: Any => {
      Logger.error("Project actor got an unhandled message: " + m)
    }
  }

  def processPendingSync(project: Project) {
    println(s"processPendingSync for project[${project.name}]")
    RecommendationsDao.sync(MainActor.SystemUser, project)
    pendingSync.map { _ =>
      SyncsDao.recordCompleted(MainActor.SystemUser, project.guid)
      pendingSync = None
    }
  }

}

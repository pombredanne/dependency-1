package com.bryzek.dependency.actors

import com.bryzek.dependency.lib.{Dependencies, GithubDependencyProviderClient}
import com.bryzek.dependency.v0.models.{Project, WatchProjectForm}
import db.{ProjectsDao, RecommendationsDao, SyncsDao, UsersDao, WatchProjectsDao}
import play.api.Logger
import play.libs.Akka
import akka.actor.Actor
import java.util.UUID
import scala.concurrent.ExecutionContext

object ProjectActor {

  object Messages {
    case class Data(guid: UUID)
    case object Sync
    case object Watch
  }

}

class ProjectActor extends Actor {

  implicit val projectExecutionContext: ExecutionContext = Akka.system.dispatchers.lookup("project-actor-context")

  var dataProject: Option[Project] = None

  def receive = {

    case ProjectActor.Messages.Data(guid) => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Data($guid)"
    ) {
      dataProject = ProjectsDao.findByGuid(guid)
    }

    case ProjectActor.Messages.Watch => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Watch"
    ) {
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

    case ProjectActor.Messages.Sync => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Sync"
    ) {
      dataProject.foreach { project =>
        SyncsDao.withStartedAndCompleted(MainActor.SystemUser, project.guid) {
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
          }

          RecommendationsDao.sync(MainActor.SystemUser, project)
        }
      }
    }

    case m: Any => {
      Logger.error("Project actor got an unhandled message: " + m)
    }
  }

}

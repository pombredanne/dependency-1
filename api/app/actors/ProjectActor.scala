package com.bryzek.dependency.actors

import com.bryzek.dependency.lib.{Dependencies, GithubDependencyProviderClient}
import com.bryzek.dependency.v0.models.{Project, WatchProjectForm}
import db.{ProjectsDao, WatchProjectsDao}
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
  var dataDependencies: Option[Dependencies] = None

  def receive = {

    case ProjectActor.Messages.Data(guid) => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Data($guid)"
    ) {
      dataProject = ProjectsDao.findByGuid(guid)
    }

    case ProjectActor.Messages.Watch => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Watch"
    ) {
      println("ProjectActor.Messages.Watch")
      dataProject.foreach { project =>
        println(" -- " + project.guid)
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
        GithubDependencyProviderClient.instance.dependencies(project).map { result =>
          result match {
            case None => {
              Logger.warn(s"project[${project.guid}] name[${project.name}]: no build file found")
            }
            case Some(dependencies) => {
              println(s" - dependencies: $dependencies")
              ProjectsDao.setDependencies(
                createdBy = MainActor.SystemUser,
                project = project,
                languages = dependencies.languages,
                libraries = dependencies.librariesAndPlugins.map(_.map { artifact =>
                  artifact.toLibraryForm(
                    resolvers = dependencies.resolvers.getOrElse(Nil),
                    crossBuildVersion = dependencies.crossBuildVersion()
                  )
                })
              )
            }
          }
          this.dataDependencies = result
        }.recover {
          case e => {
            Logger.error(s"Error fetching dependencies for project[${project.guid}] name[${project.name}]: $e")
          }
        }
      }
    }

    case m: Any => {
      Logger.error("Project actor got an unhandled message: " + m)
    }
  }

}

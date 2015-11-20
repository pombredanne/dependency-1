package com.bryzek.dependency.actors

import com.bryzek.dependency.lib.{Dependencies, GitHubClient}
import com.bryzek.dependency.v0.models.Project
import db.ProjectsDao
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object ProjectActor {

  object Messages {
    case class Data(guid: UUID)
    case object Sync
  }

}

class ProjectActor extends Actor {

  import scala.concurrent.ExecutionContext.Implicits.global

  var dataProject: Option[Project] = None
  var dataDependencies: Option[Dependencies] = None

  def receive = {

    case ProjectActor.Messages.Data(guid) => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Data($guid)"
    ) {
      dataProject = ProjectsDao.findByGuid(guid)
    }

    case ProjectActor.Messages.Sync => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Sync"
    ) {
      println(s"Sync dataProject[$dataProject]")
      dataProject.foreach { project =>
        println(s"Sync: project[${project.guid}] name[${project.name}]")
        GitHubClient.instance.dependencies(project).map { result =>
          println("RESULT:  "+ result)
          result match {
            case None => {
              println(" - project build file not found")
            }
            case Some(dependencies) => {
              println(s" - dependencies: $dependencies")
              ProjectsDao.setDependencies(
                createdBy = MainActor.SystemUser,
                project = project,
                languages = dependencies.languages,
                libraries = dependencies.librariesAndPlugins.map(_.map(_.toLibraryForm(dependencies.resolvers.getOrElse(Nil))))
              )
            }
          }
          this.dataDependencies = result
        }.recover {
          case e => {
            println(s"Error fetching dependencies for project[${project.guid}] name[${project.name}]: $e")
          }
        }
      }
    }

    case m: Any => {
      Logger.error("Project actor got an unhandled message: " + m)
    }
  }

}

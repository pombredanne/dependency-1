package com.bryzek.dependency.actors

import com.bryzek.dependency.lib.{Dependencies, GitHubClient}
import com.bryzek.dependency.v0.models.Project
import db.{ProjectsDao, UsersDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object ProjectActor {

  object Messages {
    case class Data(guid: UUID)
    case object Sync
  }

}

@javax.inject.Singleton
class ProjectActor @javax.inject.Inject() (
  usersDao: UsersDao,
  projectsDao: ProjectsDao
) extends Actor {

  import scala.concurrent.ExecutionContext.Implicits.global

  var dataProject: Option[Project] = None
  var dataDependencies: Option[Dependencies] = None

  def receive = {

    case ProjectActor.Messages.Data(guid) => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Data($guid)"
    ) {
      dataProject = projectsDao.findByGuid(guid)
    }

    case ProjectActor.Messages.Sync => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Sync"
    ) {
      dataProject.foreach { project =>
        GitHubClient.instance.dependencies(project).map { result =>
          result match {
            case None => {
              println(" - project build file not found")
            }
            case Some(dependencies) => {
              projectsDao.setDependencies(
                createdBy = usersDao.systemUser,
                project = project,
                languages = dependencies.languages,
                libraries = dependencies.librariesAndPlugins.map(_.map(_.toLibraryForm(dependencies.resolvers.getOrElse(Nil))))
              )
            }
          }
          this.dataDependencies = result
        }
      }
    }

    case m: Any => {
      Logger.error("Project actor got an unhandled message: " + m)
    }
  }

}

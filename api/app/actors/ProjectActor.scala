package com.bryzek.dependency.actors

import com.bryzek.dependency.lib.GitHubClient
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

  def receive = {

    case ProjectActor.Messages.Data(guid) => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Data($guid)"
    ) {
      dataProject = ProjectsDao.findByGuid(guid)
    }

    case ProjectActor.Messages.Sync => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Sync"
    ) {
      dataProject.foreach { project =>
        GitHubClient.instance.dependencies(project).map { result =>
          result match {
            case None => " - project build file not found"
            case Some(md) => {
              ProjectsDao.setDependencies(
                createdBy = MainActor.SystemUser,
                project = project,
                languages = Some(md.languages),
                libraries = Some(md.libraries)
              )
            }
          }
        }
      }
    }

    case m: Any => {
      Logger.error("Project actor got an unhandled message: " + m)
    }
  }

}

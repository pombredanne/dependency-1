package com.bryzek.dependency.actors

import com.bryzek.dependency.lib.GithubClientRepositoryMetadataFetcher
import com.bryzek.dependency.v0.models.Project
import io.flow.play.postgresql.Pager
import db.ProjectsDao
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object ProjectActor {

  import scala.concurrent.ExecutionContext.Implicits.global

  private lazy val token: String = scala.io.Source.fromFile("/tmp/github-token.txt", "UTF-8").mkString
  private lazy val githubClient = new GithubClientRepositoryMetadataFetcher(token)

  object Messages {
    case class Sync(guid: UUID)
    case object SyncAll
  }

  def sync(project: Project) {
    println(s"Syncing project[${project.guid}] scms[${project.scms}] name[${project.name}]")
    githubClient.repositoryMetadata(project.name).map { result =>
      result match {
        case None => " - project build file not found"
        case Some(md) => {
          println("  - languages: " + md.languages.mkString(", "))
          println("  - libraries: " + md.libraries.mkString(", "))
        }
      }
    }
  }

}

class ProjectActor extends Actor {

  def receive = {

    case ProjectActor.Messages.SyncAll => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.SyncAll"
    ) {
      Pager.eachPage { offset =>
        ProjectsDao.findAll(offset = offset)
      } { project =>
        ProjectActor.sync(project)
      }
    }

    case ProjectActor.Messages.Sync(guid) => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Sync($guid)"
    ) {
      ProjectsDao.findByGuid(guid).foreach { project =>
        ProjectActor.sync(project)
      }
    }

    case m: Any => {
      Logger.error("Project actor got an unhandled message: " + m)
    }
  }

}

package com.bryzek.dependency.actors

import db._
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object ProjectActor {

  object Messages {
    case class ProjectCreated(guid: UUID)
    case class ProjectDeleted(guid: UUID)
    case object Sync
  }

}

class ProjectActor extends Actor {

  def receive = {

    case ProjectActor.Messages.Sync => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Sync"
    ) {
      println("TODO: Iterate through and sync all projects")
    }

    case ProjectActor.Messages.ProjectCreated(guid) => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.ProjectCreated($guid)"
    ) {
      println(s"TODO: ProjectActor.ProjectCreated($guid)")
    }

    case ProjectActor.Messages.ProjectDeleted(guid) => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.ProjectDeleted($guid)"
    ) {
      println(s"TODO: ProjectActor.ProjectDeleted($guid)")
    }

    case m: Any => {
      Logger.error("Project actor got an unhandled message: " + m)
    }
  }

}

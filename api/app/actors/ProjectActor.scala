package com.bryzek.dependency.actors

import db._
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object ProjectActor {

  object Messages {
    case class Sync(guid: UUID)
    case object SyncAll
  }

}

class ProjectActor extends Actor {

  def receive = {

    case ProjectActor.Messages.SyncAll => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.SyncAll"
    ) {
      println("TODO: Iterate through and sync all projects")
    }

    case ProjectActor.Messages.Sync(guid) => Util.withVerboseErrorHandler(
      s"ProjectActor.Messages.Sync($guid)"
    ) {
      // fetch build.sbt from root repository
      println(s"TODO: ProjectActor.ProjectCreated($guid)")
    }

    case m: Any => {
      Logger.error("Project actor got an unhandled message: " + m)
    }
  }

}

package com.bryzek.dependency.actors

import io.flow.play.postgresql.Pager
import db.SyncsDao
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object SyncActor {

  sealed trait Message

  object Messages {
    case object Broadcast
    case class CreateProjectWatch(objectGuid: UUID) extends Message
    case class DeleteProjectWatch(objectGuid: UUID) extends Message
  }

}

class SyncActor extends Actor {

  private[this] val projectGuids = scala.collection.mutable.Set[UUID]()

  def receive = {

    case m @ SyncActor.Messages.Broadcast => Util.withVerboseErrorHandler(m) {
      println(s"Sync broadcasting")
      projectGuids.foreach { projectGuid =>
        println(s"Broadcast to projectGuid[$projectGuid]")
      }
    }

    case m @ SyncActor.Messages.CreateProjectWatch(projectGuid) => Util.withVerboseErrorHandler(m) {
      projectGuids += projectGuid
      println(s"Sync watch created for projectGuid[$projectGuid]")
    }

    case m @ SyncActor.Messages.DeleteProjectWatch(projectGuid) => Util.withVerboseErrorHandler(m) {
      projectGuids -= projectGuid
      println(s"Sync watch deleted for projectGuid[$projectGuid]")
    }

    case m: Any => {
      Logger.error("Sync actor got an unhandled message: " + m)
    }
  }

}

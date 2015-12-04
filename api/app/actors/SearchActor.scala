package com.bryzek.dependency.actors

import com.bryzek.dependency.v0.models.{BinarySummary, LibrarySummary, ProjectSummary}
import db.{BinariesDao, ItemForm, ItemsDao, LibrariesDao, ProjectsDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object SearchActor {

  sealed trait Message

  object Messages {
    case class SyncBinary(guid: UUID) extends Message
    case class SyncLibrary(guid: UUID) extends Message
    case class SyncProject(guid: UUID) extends Message
  }

}

class SearchActor extends Actor {

  def receive = {

    case SearchActor.Messages.SyncBinary(guid) => Util.withVerboseErrorHandler(
      s"SearchActor.Messages.SyncBinary($guid)"
    ) {
      BinariesDao.findByGuid(guid) match {
        case None => ItemsDao.softDeleteByObjectGuid(MainActor.SystemUser, guid)
        case Some(binary) => ItemsDao.upsertBinary(MainActor.SystemUser, binary)
      }
    }

    case SearchActor.Messages.SyncLibrary(guid) => Util.withVerboseErrorHandler(
      s"SearchActor.Messages.SyncLibrary($guid)"
    ) {
      LibrariesDao.findByGuid(guid) match {
        case None => ItemsDao.softDeleteByObjectGuid(MainActor.SystemUser, guid)
        case Some(library) => ItemsDao.upsertLibrary(MainActor.SystemUser, library)
      }
    }

    case SearchActor.Messages.SyncProject(guid) => Util.withVerboseErrorHandler(
      s"SearchActor.Messages.SyncProject($guid)"
    ) {
      ProjectsDao.findByGuid(guid) match {
        case None => ItemsDao.softDeleteByObjectGuid(MainActor.SystemUser, guid)
        case Some(project) => ItemsDao.upsertProject(MainActor.SystemUser, project)
      }
    }

    case m: Any => {
      Logger.error("Search actor got an unhandled message: " + m)
    }
  }

}

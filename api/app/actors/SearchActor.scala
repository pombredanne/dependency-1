package com.bryzek.dependency.actors

import com.bryzek.dependency.v0.models.{BinarySummary, LibrarySummary, ProjectSummary}
import db.{Authorization, BinariesDao, ItemForm, ItemsDao, LibrariesDao, ProjectsDao}
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

class SearchActor extends Actor with Util {

  def receive = {

    case m @ SearchActor.Messages.SyncBinary(guid) => withVerboseErrorHandler(m) {
      BinariesDao.findByGuid(guid) match {
        case None => ItemsDao.softDeleteByObjectGuid(Authorization.All, MainActor.SystemUser, guid)
        case Some(binary) => ItemsDao.upsertBinary(MainActor.SystemUser, binary)
      }
    }

    case m @ SearchActor.Messages.SyncLibrary(guid) => withVerboseErrorHandler(m) {
      LibrariesDao.findByGuid(guid) match {
        case None => ItemsDao.softDeleteByObjectGuid(Authorization.All, MainActor.SystemUser, guid)
        case Some(library) => ItemsDao.upsertLibrary(MainActor.SystemUser, library)
      }
    }

    case m @ SearchActor.Messages.SyncProject(guid) => withVerboseErrorHandler(m) {
      ProjectsDao.findByGuid(guid) match {
        case None => ItemsDao.softDeleteByObjectGuid(Authorization.All, MainActor.SystemUser, guid)
        case Some(project) => ItemsDao.upsertProject(MainActor.SystemUser, project)
      }
    }

    case m: Any => logUnhandledMessage(m)
  }

}

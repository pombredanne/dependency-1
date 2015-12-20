package com.bryzek.dependency.actors

import com.bryzek.dependency.v0.models.{Binary, BinaryForm}
import com.bryzek.dependency.api.lib.DefaultBinaryVersionProvider
import io.flow.play.postgresql.Pager
import db.{Authorization, BinariesDao, BinaryVersionsDao, ItemsDao, ProjectBinariesDao, SyncsDao, UsersDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object BinaryActor {

  object Messages {
    case class Data(guid: UUID)
    case object Sync
    case object Deleted
  }

}

class BinaryActor extends Actor with Util {

  var dataBinary: Option[Binary] = None

  def receive = {

    case m @ BinaryActor.Messages.Data(guid: UUID) => withVerboseErrorHandler(m) {
      dataBinary = BinariesDao.findByGuid(Authorization.All, guid)
    }

    case m @ BinaryActor.Messages.Sync => withVerboseErrorHandler(m) {
      dataBinary.foreach { binary =>
        SyncsDao.withStartedAndCompleted(MainActor.SystemUser, "binary", binary.guid) {
          DefaultBinaryVersionProvider.versions(binary.name).foreach { version =>
            BinaryVersionsDao.upsert(UsersDao.systemUser, binary.guid, version.value)
          }
        }

        sender ! MainActor.Messages.BinarySyncCompleted(binary.guid)
      }
    }

    case m @ BinaryActor.Messages.Deleted => withVerboseErrorHandler(m) {
      dataBinary.foreach { binary =>
        ItemsDao.softDeleteByObjectGuid(Authorization.All, MainActor.SystemUser, binary.guid)

        Pager.eachPage { offset =>
          ProjectBinariesDao.findAll(Authorization.All, binaryGuid = Some(binary.guid), offset = offset)
        } { projectBinary =>
          ProjectBinariesDao.removeBinary(MainActor.SystemUser, projectBinary)
          sender ! MainActor.Messages.ProjectBinarySync(projectBinary.project.guid, projectBinary.guid)
        }
      }
    }

    case m: Any => logUnhandledMessage(m)
  }

}

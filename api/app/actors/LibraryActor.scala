package com.bryzek.dependency.actors

import com.bryzek.dependency.v0.models.{Library, LibraryForm, VersionForm}
import com.bryzek.dependency.lib.DefaultLibraryArtifactProvider
import io.flow.play.postgresql.Pager
import db.{LibrariesDao, LibraryVersionsDao, UsersDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object LibraryActor {

  object Messages {
    case class Data(guid: UUID)
    case object Sync
  }

}

class LibraryActor extends Actor {

  var dataLibrary: Option[Library] = None

  def receive = {

    case LibraryActor.Messages.Data(guid: UUID) => Util.withVerboseErrorHandler(
      s"LibraryActor.Messages.Data($guid)"
    ) {
      dataLibrary = LibrariesDao.findByGuid(guid)
      self ! LibraryActor.Messages.Sync
    }

    case LibraryActor.Messages.Sync => Util.withVerboseErrorHandler(
      s"LibraryActor.Messages.Sync"
    ) {
      dataLibrary.foreach { lib =>
        println(s"Syncing library[$lib]")
        DefaultLibraryArtifactProvider().artifacts(lib).map { version =>
          println(s" groupId[${lib.groupId}] artifactId[${lib.artifactId}] version[${version.tag.value}] crossBuilt[${version.crossBuildVersion.map(_.value).getOrElse("")}]")
          LibraryVersionsDao.upsert(
            createdBy = UsersDao.systemUser,
            libraryGuid = lib.guid,
            form = VersionForm(version.tag.value, version.crossBuildVersion.map(_.value))
          )
        }
      }
    }

    case m: Any => {
      Logger.error("Library actor got an unhandled mesage: " + m)
    }
  }

}

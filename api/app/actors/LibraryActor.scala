package com.bryzek.dependency.actors

import com.bryzek.dependency.v0.models.{Library, LibraryForm, Resolver, VersionForm}
import com.bryzek.dependency.api.lib.DefaultLibraryArtifactProvider
import db.{Authorization, LibrariesDao, LibraryVersionsDao, ResolversDao, SyncsDao, UsersDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object LibraryActor {

  object Messages {
    case class Data(guid: UUID)
    case object Sync
  }

}

class LibraryActor extends Actor with Util {

  var dataLibrary: Option[Library] = None

  def receive = {

    case m @ LibraryActor.Messages.Data(guid: UUID) => withVerboseErrorHandler(m) {
      dataLibrary = LibrariesDao.findByGuid(Authorization.All, guid)
    }

    case m @ LibraryActor.Messages.Sync => withVerboseErrorHandler(m) {
      dataLibrary.foreach { lib =>
        SyncsDao.withStartedAndCompleted(MainActor.SystemUser, lib.guid) {
          ResolversDao.findByGuid(Authorization.All, lib.resolver.guid).map { resolver =>
            DefaultLibraryArtifactProvider().resolve(
              resolver = resolver,
              groupId = lib.groupId,
              artifactId = lib.artifactId
            ).map { resolution =>
              println(s"Library[${lib.groupId}.${lib.artifactId}] resolver[${lib.resolver}] -- found[${resolution.resolver}]")

              resolution.versions.foreach { version =>
                LibraryVersionsDao.upsert(
                  createdBy = MainActor.SystemUser,
                  libraryGuid = lib.guid,
                  form = VersionForm(version.tag.value, version.crossBuildVersion.map(_.value))
                )
              }
            }
          }
        }

        // TODO: Should we only send if something changed?
        sender ! MainActor.Messages.LibrarySyncCompleted(lib.guid)
      }
    }

    case m: Any => logUnhandledMessage(m)
  }

}

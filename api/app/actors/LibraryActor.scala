package com.bryzek.dependency.actors

import com.bryzek.dependency.v0.models.{Library, LibraryForm, Resolver, VersionForm}
import com.bryzek.dependency.lib.DefaultLibraryArtifactProvider
import io.flow.play.postgresql.Pager
import db.{LibrariesDao, LibraryVersionsDao, ResolversDao, SyncsDao, UsersDao}
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
      dataLibrary = LibrariesDao.findByGuid(guid)
    }

    case m @ LibraryActor.Messages.Sync => withVerboseErrorHandler(m) {
      dataLibrary.foreach { lib =>
        val user = UsersDao.findByGuid(lib.audit.createdBy.guid)

        SyncsDao.withStartedAndCompleted(MainActor.SystemUser, lib.guid) {
          DefaultLibraryArtifactProvider().artifacts(
            lib,
            user = user,
            resolver = lib.resolver
          ).map { resolution =>
            println(s"Library[${lib.groupId}.${lib.artifactId}] resolver[${lib.resolver}] -- found[${resolution.resolver}]")

            if (lib.resolver.map(_.guid) != Some(resolution.resolver.guid)) {
              LibrariesDao.update(
                MainActor.SystemUser,
                lib,
                LibraryForm(
                  groupId = lib.groupId,
                  artifactId = lib.artifactId,
                  resolverGuid = Some(resolution.resolver.guid)
                )
              )
            }

            resolution.versions.foreach { version =>
              LibraryVersionsDao.upsert(
                createdBy = MainActor.SystemUser,
                libraryGuid = lib.guid,
                form = VersionForm(version.tag.value, version.crossBuildVersion.map(_.value))
              )
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

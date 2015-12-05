package com.bryzek.dependency.actors

import com.bryzek.dependency.v0.models.{Library, LibraryForm, Resolver, VersionForm}
import com.bryzek.dependency.lib.DefaultLibraryArtifactProvider
import io.flow.play.postgresql.Pager
import db.{LibrariesDao, LibraryVersionsDao, ResolversDao, SyncsDao}
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
      var resolvers = scala.collection.mutable.Map[UUID, Seq[Resolver]]()

      dataLibrary.foreach { lib =>
        SyncsDao.withStartedAndCompleted(MainActor.SystemUser, lib.guid) {
          val userResolvers = resolvers.get(lib.audit.createdBy.guid).getOrElse {
            val all = ResolversDao.findAll(
              userGuid = Some(lib.audit.createdBy.guid)
            )
            resolvers +== (lib.audit.createdBy.guid -> all)
            all
          }

          // println(s"Syncing library[$lib] for user[${lib.audit.createdBy.guid}] resolvers[${userResolvers.map(_.uri)}]")
          DefaultLibraryArtifactProvider().artifacts(lib, userResolvers).map { version =>
            // println(s" groupId[${lib.groupId}] artifactId[${lib.artifactId}] version[${version.tag.value}] crossBuilt[${version.crossBuildVersion.map(_.value).getOrElse("")}]")
            LibraryVersionsDao.upsert(
              createdBy = MainActor.SystemUser,
              libraryGuid = lib.guid,
              form = VersionForm(version.tag.value, version.crossBuildVersion.map(_.value))
            )
          }
        }

        // TODO: Should we only send if something changed?
        sender ! MainActor.Messages.LibrarySyncCompleted(lib.guid)
      }
    }

    case m: Any => logUnhandledMessage(m)
  }

}

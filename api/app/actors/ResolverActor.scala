package com.bryzek.dependency.actors

import com.bryzek.dependency.v0.models.{Resolver, Visibility}
import io.flow.play.postgresql.Pager
import db.{Authorization, LibrariesDao, ProjectLibrariesDao, OrganizationsDao, SubscriptionsDao, ResolversDao}
import akka.actor.Actor
import java.util.UUID
import scala.concurrent.ExecutionContext

object ResolverActor {

  trait Message

  object Messages {
    case class Data(guid: UUID) extends Message
    case object Created extends Message
    case object Sync extends Message
    case object Deleted extends Message
  }

}

class ResolverActor extends Actor with Util {

  var dataResolver: Option[Resolver] = None

  def receive = {

    case m @ ResolverActor.Messages.Data(guid) => withVerboseErrorHandler(m.toString) {
      dataResolver = ResolversDao.findByGuid(Authorization.All, guid)
    }

    case m @ ResolverActor.Messages.Created => withVerboseErrorHandler(m.toString) {
      sync()
    }

    case m @ ResolverActor.Messages.Sync => withVerboseErrorHandler(m.toString) {
      sync()
    }

    case m @ ResolverActor.Messages.Deleted => withVerboseErrorHandler(m.toString) {
      context.stop(self)
    }

    case m: Any => logUnhandledMessage(m)
  }

  def sync() {
    dataResolver.foreach { resolver =>
      // Trigger resolution for any project libraries that are currently not resolved.
      val auth = (resolver.organization, resolver.visibility) match {
        case (None, _) => Authorization.All
        case (Some(org), Visibility.Public | Visibility.UNDEFINED(_)) => {
          Authorization.All
        }
        case (Some(org), Visibility.Private) => {
          Authorization.Organization(org.guid)
        }
      }

      Pager.eachPage { offset =>
        ProjectLibrariesDao.findAll(auth, hasLibrary = Some(false), offset = offset)
      } { projectLibrary =>
        sender ! MainActor.Messages.ProjectLibrarySync(projectLibrary.project.guid, projectLibrary.guid)
      }
    }
  }

}

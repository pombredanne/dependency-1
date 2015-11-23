package com.bryzek.dependency.actors

import com.bryzek.dependency.v0.models.Language
import io.flow.play.postgresql.Pager
import db.{LanguagesDao, LanguageVersionsDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object LanguageActor {

  object Messages {
    case class Data(guid: UUID)
    case object Sync
  }

}

class LanguageActor extends Actor {

  var dataLanguage: Option[Language] = None

  def receive = {

    case LanguageActor.Messages.Data(guid: UUID) => Util.withVerboseErrorHandler(
      s"LanguageActor.Messages.Data($guid)"
    ) {
      dataLanguage = LanguagesDao.findByGuid(guid)
      self ! LanguageActor.Messages.Sync
    }

    case LanguageActor.Messages.Sync => Util.withVerboseErrorHandler(
      s"LanguageActor.Messages.Sync"
    ) {
      dataLanguage.foreach { lang =>
        // TODO: fetch all versions for this language and store them
      }
    }

    case m: Any => {
      Logger.error("Language actor got an unhandled mesage: " + m)
    }
  }

}

package com.bryzek.dependency.actors

import com.bryzek.dependency.lib.DefaultLanguageVersionProvider
import com.bryzek.dependency.v0.models.Language
import io.flow.play.postgresql.Pager
import db.{LanguagesDao, LanguageVersionsDao, UsersDao}
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
        DefaultLanguageVersionProvider.versions(lang.name).foreach { version =>
          // TODO: fetch all versions for this language and store them
          println(s"Store version[${version.value}] from lang[$lang]")
          LanguageVersionsDao.upsert(UsersDao.systemUser, lang.guid, version.value)
        }
      }
    }

    case m: Any => {
      Logger.error("Language actor got an unhandled mesage: " + m)
    }
  }

}

package com.bryzek.dependency.actors

import io.flow.play.postgresql.Pager
import db.{SubscriptionsDao, UsersDao}
import com.bryzek.dependency.v0.models.Publication
import com.bryzek.dependency.lib.{Email, Person}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object EmailActor {

  object Messages {
    case object ProcessDailySummary
  }

}

class EmailActor extends Actor with Util {

  def receive = {

    case m @ EmailActor.Messages.ProcessDailySummary => withVerboseErrorHandler(m) {
      Pager.eachPage { offset =>
        SubscriptionsDao.findAll(
          publication = Some(Publication.DailySummary),
          offset = offset
        )
      } { subscription =>
        println(s"subscription: $subscription")
        UsersDao.findByGuid(subscription.user.guid).foreach { user =>
          println(s" - user[${user.guid}] email[${user.email}]")
          user.email match {
            case None => {
              println(" - user does not have an email address")
            }
            case Some(email) => {
              println(s" - user email is $email")
            }
          }
        }
      }
    }

  }

}

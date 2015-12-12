package com.bryzek.dependency.actors

import io.flow.play.postgresql.Pager
import db.{LastEmailForm, LastEmailsDao, SubscriptionsDao, UsersDao}
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
      EmailMessage(Publication.DailySummary).deliver()
    }

  }

}

case class EmailMessage(publication: Publication) {

  private val minHoursSinceLastEmail = 23

  def deliver() {
    Pager.eachPage { offset =>
      SubscriptionsDao.findAll(
        publication = Some(publication),
        minHoursSinceLastEmail = Some(minHoursSinceLastEmail),
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
            val lastEmail = LastEmailsDao.findByUserGuidAndPublication(user.guid, publication)
            println(s" - user email is $email - last email sent at: " + lastEmail.map(_.audit.createdAt).getOrElse(""))
            LastEmailsDao.record(
              MainActor.SystemUser,
              LastEmailForm(
                userGuid = user.guid,
                publication = publication
              )
            )
          }
        }
      }
    }

  }

}

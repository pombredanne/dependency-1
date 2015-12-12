package com.bryzek.dependency.actors

import io.flow.play.util.DefaultConfig
import io.flow.play.postgresql.Pager
import db.{LastEmailForm, LastEmailsDao, RecommendationsDao, SubscriptionsDao, UsersDao}
import com.bryzek.dependency.v0.models.Publication
import com.bryzek.dependency.api.lib.{Email, Person}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object EmailActor {

  private[actors] val SubscriptionsUrl = DefaultConfig.requiredString("dependency.www.host") + "/subscriptions"

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
        Person.fromUser(user) match {
          case None => {
            println(" - user does not have an email address")
          }
          case Some(person) => {
            val lastEmail = LastEmailsDao.findByUserGuidAndPublication(user.guid, publication)
            val recommendations = RecommendationsDao.findAll(
              userGuid = Some(user.guid),
              limit = 100
            )

            println(s" - person is $person - last email sent at: " + lastEmail.map(_.audit.createdAt).getOrElse(""))

            Email.sendHtml(
              to = person,
              subject = s"Daily Summary",
              body = views.html.emails.dailySummary(
                person = person,
                recommendations = recommendations,
                lastEmail = lastEmail,
                subscriptionsUrl = EmailActor.SubscriptionsUrl
              ).toString
            )
            
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

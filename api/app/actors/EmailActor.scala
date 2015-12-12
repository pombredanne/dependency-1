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
            Email.sendHtml(
              to = person,
              subject = s"Daily Summary",
              body = generate(user.guid, person).toString
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

  def generate(userGuid: UUID, person: Person): play.twirl.api.HtmlFormat.Appendable = {
    val lastEmail = LastEmailsDao.findByUserGuidAndPublication(userGuid, publication)
    val recommendations = RecommendationsDao.findAll(
      userGuid = Some(userGuid),
      limit = 100
    )
    println(s" - generate($userGuid, $person). last email sent at: " + lastEmail.map(_.audit.createdAt).getOrElse(""))

    views.html.emails.dailySummary(
      person = person,
      recommendations = recommendations,
      lastEmail = lastEmail,
      subscriptionsUrl = EmailActor.SubscriptionsUrl
    )
  }

}

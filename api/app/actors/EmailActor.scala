package com.bryzek.dependency.actors

import io.flow.play.util.DefaultConfig
import io.flow.play.postgresql.Pager
import io.flow.user.v0.models.User
import db.{LastEmail, LastEmailForm, LastEmailsDao, RecommendationsDao, SubscriptionsDao, UsersDao}
import com.bryzek.dependency.v0.models.Publication
import com.bryzek.dependency.api.lib.{Email, Person, Urls}
import org.joda.time.{DateTime, DateTimeZone}
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
      BatchEmailProcessor(
        publication = Publication.DailySummary,
        minHoursSinceLastEmail = 23
      ) { user =>
        DailySummaryEmailMessage(user)
      }
    }

  }

}

case class BatchEmailProcessor(
  publication: Publication,
  minHoursSinceLastEmail: Int
) (
  userGenerator: User => EmailMessageGenerator
) {

  def process() {
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
        val generator = userGenerator(user)
        if (generator.shouldSend()) {
          Person.fromUser(user).map { person =>

            // Record before send in case of crash - prevent loop of
            // emails.
            LastEmailsDao.record(
              MainActor.SystemUser,
              LastEmailForm(
                userGuid = user.guid,
                publication = publication
              )
            )

            Email.sendHtml(
              to = person,
              subject = generator.subject(),
              body = generator.body()
            )
          }
        }
      }
    }
  }
}

trait EmailMessageGenerator {
  def shouldSend(): Boolean
  def subject(): String
  def body(): String
}

/**
  * Class which generates email message
  */
case class DailySummaryEmailMessage(user: User) extends EmailMessageGenerator {

  private[this] val PreferredHourToSendEst = {
    val value = DefaultConfig.requiredString("com.bryzek.dependency.api.email.daily.summary.hour.est").toInt
    assert( value >= 0 && value < 23 )
    value
  }

  private lazy val lastEmail = LastEmailsDao.findByUserGuidAndPublication(user.guid, Publication.DailySummary)

  /**
   * We send anytime within the preferred hour - Since we select
   * people we have not yet emailed in 23 hours - this gives us an
   * hour each day in which for the jobs to run to get the email
   * sent. If we are down for the entire hour - we will just pick up
   * tomorrow. Main priority here is to ensure email is consistently
   * sent in the morning while keeping code simple.
   */
  override def shouldSend(): Boolean = {
    PreferredHourToSendEst == (new DateTime()).toDateTime(DateTimeZone.forID("America/New_York")).getHourOfDay
  }

  override def subject() = "Daily Summary"

  override def body() = {
    val recommendations = RecommendationsDao.findAll(
      userGuid = Some(user.guid),
      limit = 250
    )
    val (newRecommendations, oldRecommendations) = lastEmail match {
      case None => (recommendations, Nil)
      case Some(email) => {
        (
          recommendations.filter { !_.audit.createdAt.isBefore(email.audit.createdAt) },
          recommendations.filter { _.audit.createdAt.isBefore(email.audit.createdAt) }
        )
      }
    }

    views.html.emails.dailySummary(
      name = user.name,
      newRecommendations = newRecommendations,
      oldRecommendations = oldRecommendations,
      lastEmail = lastEmail,
      urls = Urls()
    ).toString
  }

}

package com.bryzek.dependency.actors

import io.flow.play.util.DefaultConfig
import io.flow.play.postgresql.Pager
import io.flow.user.v0.models.User
import db.{Authorization, LastEmail, LastEmailForm, LastEmailsDao, RecommendationsDao, SubscriptionsDao, UserIdentifiersDao, UsersDao}
import com.bryzek.dependency.v0.models.{Publication, Subscription}
import com.bryzek.dependency.lib.Urls
import com.bryzek.dependency.api.lib.{Email, Recipient}
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

    /**
      * Selects people to whom we delivery email by:
      *
      *   a. never received a daily summary before. We want to leave
      *      at least 1 hour from time of registration to time of first
      *      email to allow them to add projects.
      *
      *   b. It's between 7 and 8am EST - standard delivery
      *      time. Deliver email to anybody whose last email was >= 23
      *      hours ago
      *
      *   c. It's not 7am - we want to deliver email only to catch
      *      up. So in this case we select anybody with a last email
      *      delivered more than 24 hours ago.
      */
    case m @ EmailActor.Messages.ProcessDailySummary => withVerboseErrorHandler(m) {
      BatchEmailProcessor(
        Publication.DailySummary,
        Pager.create { offset =>
          SubscriptionsDao.findAll(
            publication = Some(Publication.DailySummary),
            minHoursSinceLastEmail = Some(23),
            offset = offset
          )
        }
      ) { recipient =>
        DailySummaryEmailMessage(recipient)
      }.process()
    }

  }

}

case class BatchEmailProcessor(
  publication: Publication,
  subscriptions: Iterator[Subscription]
) (
  generator: Recipient => EmailMessageGenerator
) {

  def process() {
    subscriptions.foreach { subscription =>
      println(s"subscription: $subscription")
      UsersDao.findByGuid(subscription.user.guid).foreach { user =>
        println(s" - user[${user.guid}] email[${user.email}]")
        Recipient.fromUser(user).map { DailySummaryEmailMessage(_) }.map { generator =>
          if (generator.shouldSend()) {
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
              to = generator.recipient,
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
  def recipient(): Recipient
  def subject(): String
  def body(): String
}

/**
  * Class which generates email message
  */
case class DailySummaryEmailMessage(recipient: Recipient) extends EmailMessageGenerator {

  private[this] val PreferredHourToSendEst = {
    val value = DefaultConfig.requiredString("com.bryzek.dependency.api.email.daily.summary.hour.est").toInt
    assert( value >= 0 && value < 23 )
    value
  }

  private val MaxRecommendations = 250

  private lazy val lastEmail = LastEmailsDao.findByUserGuidAndPublication(recipient.userGuid, Publication.DailySummary)

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
      Authorization.User(recipient.userGuid),
      userGuid = Some(recipient.userGuid),
      limit = Some(MaxRecommendations)
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
      recipient = recipient,
      newRecommendations = newRecommendations,
      oldRecommendations = oldRecommendations,
      lastEmail = lastEmail,
      urls = Urls()
    ).toString
  }

}

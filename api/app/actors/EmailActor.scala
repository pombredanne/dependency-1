package com.bryzek.dependency.actors

import io.flow.play.util.DefaultConfig
import io.flow.play.postgresql.Pager
import io.flow.user.v0.models.User
import db.{LastEmail, LastEmailForm, LastEmailsDao, RecommendationsDao, SubscriptionsDao, UsersDao}
import com.bryzek.dependency.v0.models.Publication
import com.bryzek.dependency.api.lib.{Email, Person, Urls}
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
        val lastEmail = LastEmailsDao.findByUserGuidAndPublication(user.guid, publication)
        val generator = userGenerator(user)
        if (generator.shouldSend(lastEmail)) {
          Person.fromUser(user).map { person =>

            Email.sendHtml(
              to = person,
              subject = generator.subject(),
              body = generator.body()
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

trait EmailMessageGenerator {
  def shouldSend(lastEmail: Option[LastEmail]): Boolean
  def subject(): String
  def body(): String
}

/**
  * Class which generates email message
  */
case class DailySummaryEmailMessage(user: User) extends EmailMessageGenerator {

  private val minHoursSinceLastEmail = 23
  private lazy val lastEmail = LastEmailsDao.findByUserGuidAndPublication(user.guid, Publication.DailySummary)

  override def shouldSend(lastEmail: Option[LastEmail]) = true

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

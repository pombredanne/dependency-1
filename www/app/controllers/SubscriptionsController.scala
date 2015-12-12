package controllers

import com.bryzek.dependency.v0.models.{Publication, SubscriptionForm}
import com.bryzek.dependency.www.lib.DependencyClientProvider
import io.flow.play.clients.UserTokensClient
import io.flow.play.util.{Pagination, PaginatedCollection}
import java.util.UUID
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

object Subscriptions {

  case class UserPublication(publication: Publication, isSubscribed: Boolean) {
    val label = publication match {
      case Publication.DailySummary => "Email me a daily summary of dependencies to upgrade"
      case Publication.UNDEFINED(key) => key
    }
  }

}

class SubscriptionsController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def section = Some(com.bryzek.dependency.www.lib.Section.Subscriptions)

  def index() = Identified.async { implicit request =>
    for {
      subscriptions <- dependencyClient(request).subscriptions.get(
        userGuid = Some(request.user.guid),
        limit = Publication.all.size + 1
      )
    } yield {
      val userPublications = Publication.all.map { p =>
        Subscriptions.UserPublication(
          publication = p,
          isSubscribed = !subscriptions.find(_.publication == p).isEmpty
        )
      }
      Ok(views.html.subscriptions.index(uiData(request), userPublications))
    }
  }

  def postToggle(publication: Publication) = Identified.async { implicit request =>
    dependencyClient(request).subscriptions.get(
      userGuid = Some(request.user.guid),
      publication = Some(publication)
    ).flatMap { subscriptions =>
      subscriptions.headOption match {
        case None => {
          dependencyClient(request).subscriptions.post(
            SubscriptionForm(
              userGuid = request.user.guid,
              publication = publication
            )
          ).map { _ =>
            Redirect(routes.SubscriptionsController.index()).flashing("success" -> "Subscription added")
          }
        }
        case Some(subscription) => {
          dependencyClient(request).subscriptions.deleteByGuid(subscription.guid).map { _ =>
            Redirect(routes.SubscriptionsController.index()).flashing("success" -> "Subscription removed")
          }
        }
      }
    }
  }

}

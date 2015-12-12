package db

import com.bryzek.dependency.v0.models.Publication
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class SubscriptionsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "upsert" in {
    val form = createSubscriptionForm()
    val subscription1 = SubscriptionsDao.create(systemUser, form).right.get

    val subscription2 = SubscriptionsDao.upsert(systemUser, form)
    subscription1.guid must be(subscription2.guid)

    val newSubscription = UUID.randomUUID.toString
    val subscription3 = createSubscription()

    subscription2.guid must not be(subscription3.guid)
  }

  "findByGuid" in {
    val subscription = createSubscription()
    SubscriptionsDao.findByGuid(subscription.guid).map(_.guid) must be(
      Some(subscription.guid)
    )

    SubscriptionsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findByUserGuidAndPublication" in {
    val subscription = createSubscription()
    SubscriptionsDao.findByUserGuidAndPublication(subscription.user.guid, subscription.publication).map(_.guid) must be(
      Some(subscription.guid)
    )

    SubscriptionsDao.findByUserGuidAndPublication(UUID.randomUUID, subscription.publication).map(_.guid) must be(None)
    SubscriptionsDao.findByUserGuidAndPublication(subscription.user.guid, Publication.UNDEFINED("other")).map(_.guid) must be(None)
  }

  "findAll by guids" in {
    val subscription1 = createSubscription()
    val subscription2 = createSubscription()

    SubscriptionsDao.findAll(guids = Some(Seq(subscription1.guid, subscription2.guid))).map(_.guid) must be(
      Seq(subscription1.guid, subscription2.guid)
    )

    SubscriptionsDao.findAll(guids = Some(Nil)) must be(Nil)
    SubscriptionsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    SubscriptionsDao.findAll(guids = Some(Seq(subscription1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(subscription1.guid))
  }

  "findAll by minHoursSinceLastEmail" in {
    val user = createUser()
    val subscription = createSubscription(
      createSubscriptionForm(user = user, publication = Publication.DailySummary)
    )

    SubscriptionsDao.findAll(
      guid = Some(subscription.guid),
      minHoursSinceLastEmail = Some(1)
    ).map(_.guid) must be(Seq(subscription.guid))

    createLastEmail(createLastEmailForm(user = user, publication = Publication.DailySummary))
    
    SubscriptionsDao.findAll(
      guid = Some(subscription.guid),
      minHoursSinceLastEmail = Some(1)
    ) must be(Nil)
  }

}

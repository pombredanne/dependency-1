package db

import com.bryzek.dependency.v0.models.{Publication, Subscription, SubscriptionForm}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object SubscriptionsDao {

  private[this] val BaseQuery = s"""
    select subscriptions.guid,
           subscriptions.user_guid as subscriptions_user_guid,
           subscriptions.publication,
           ${AuditsDao.all("subscriptions")}
      from subscriptions
     where true
  """

  private[this] val InsertQuery = """
    insert into subscriptions
    (guid, user_guid, publication, updated_by_guid, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {publication}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[db] def validate(
    form: SubscriptionForm
  ): Seq[String] = {
    val userErrors = UsersDao.findByGuid(form.userGuid) match {
      case None => Seq("User not found")
      case Some(_) => Nil
    }

    val publicationErrors = form.publication match {
      case Publication.UNDEFINED(_) => Seq("Invalid publication")
      case _ => Nil
    }

    userErrors ++ publicationErrors
  }

  def upsert(createdBy: User, form: SubscriptionForm): Subscription = {
    findByUserGuidAndPublication(form.userGuid, form.publication).getOrElse {
      create(createdBy, form) match {
        case Left(errors) => {
          findByUserGuidAndPublication(form.userGuid, form.publication).getOrElse {
            sys.error(errors.mkString(", "))
          }
        }
        case Right(subscription) => subscription
      }
    }
  }

  def create(createdBy: User, form: SubscriptionForm): Either[Seq[String], Subscription] = {
    validate(form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'user_guid -> form.userGuid,
            'publication -> form.publication.toString,
            'created_by_guid -> createdBy.guid
          ).execute()
        }

        Right(
          findByGuid(guid).getOrElse {
            sys.error("Failed to create subscription")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, subscription: Subscription) {
    SoftDelete.delete("subscriptions", deletedBy.guid, subscription.guid)
  }

  def findByUserGuidAndPublication(
    userGuid: UUID,
    publication: Publication
  ): Option[Subscription] = {
    findAll(
      userGuid = Some(userGuid),
      publication = Some(publication),
      limit = 1
    ).headOption
  }

  def findByGuid(guid: UUID): Option[Subscription] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
    publication: Option[Publication] = None,
    minHoursSinceLastEmail: Option[Int] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Subscription] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v =>  "and subscriptions.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("subscriptions.guid", _) },
      userGuid.map { v => "and subscriptions.user_guid = {user_guid}::uuid" },
      publication.map { v => "and subscriptions.publication = {publication}" },
      minHoursSinceLastEmail.map { v => """
        and not exists (select 1
                          from last_emails
                         where last_emails.deleted_at is null
                           and last_emails.user_guid = subscriptions.user_guid
                           and last_emails.publication = subscriptions.publication
                           and last_emails.created_at > now() - interval '1 hour' * {min_hours})
      """.trim },
      isDeleted.map(Filters.isDeleted("subscriptions", _)),
      Some(s"order by subscriptions.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      publication.map('publication -> _.toString),
      minHoursSinceLastEmail.map('min_hours -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Subscription.table("subscriptions").*
      )
    }
  }

}

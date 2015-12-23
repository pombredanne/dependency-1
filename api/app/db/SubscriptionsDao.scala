package db

import com.bryzek.dependency.v0.models.{Publication, Subscription, SubscriptionForm}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Query, OrderBy, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object SubscriptionsDao {

  private[this] val BaseQuery = Query(s"""
    select subscriptions.guid,
           subscriptions.user_guid as subscriptions_user_guid,
           subscriptions.publication,
           ${AuditsDao.all("subscriptions")}
      from subscriptions
  """)

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
    identifier: Option[String] = None,
    publication: Option[Publication] = None,
    minHoursSinceLastEmail: Option[Int] = None,
    minHoursSinceRegistration: Option[Int] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy.parseOrError("subscriptions.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Subscription] = {
    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "subscriptions",
        auth = Clause.True, // TODO
        guid = guid,
        guids = guids,
        orderBy = orderBy.sql,
        isDeleted = isDeleted,
        limit = Some(limit),
        offset = offset
      ).
        uuid("subscriptions.user_guid", userGuid).
        text("subscriptions.publication", publication).
        condition(
          minHoursSinceLastEmail.map { v => """
            not exists (select 1
                          from last_emails
                         where last_emails.deleted_at is null
                           and last_emails.user_guid = subscriptions.user_guid
                           and last_emails.publication = subscriptions.publication
                           and last_emails.created_at > now() - interval '1 hour' * {min_hours}::int)
          """.trim }
        ).bind("min_hours", minHoursSinceLastEmail).
        condition(
          minHoursSinceRegistration.map { v => """
            exists (select 1
                      from users
                     where users.deleted_at is null
                       and users.guid = subscriptions.user_guid
                       and users.created_at <= now() - interval '1 hour' * {min_hours_since_registration}::int)
          """.trim }
        ).bind("min_hours_since_registration", minHoursSinceRegistration).
        subquery("subscriptions.user_guid", "identifier", identifier, { bindVar =>
          s"select user_guid from user_identifiers where deleted_at is null and value = trim({$bindVar})"
        }).
        as(
          com.bryzek.dependency.v0.anorm.parsers.Subscription.table("subscriptions").*
        )
    }
  }

}

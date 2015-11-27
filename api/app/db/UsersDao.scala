package db

import io.flow.common.v0.models.Audit
import io.flow.play.postgresql.{AuditsDao, Filters, OrderBy}
import io.flow.user.v0.models.{Name, User, UserForm}
import java.util.UUID
import anorm._
import play.api.db._
import play.api.Play.current

object UsersDao {

  private[db] val SystemEmailAddress = "system@bryzek.com"
  private[db] val AnonymousEmailAddress = "anonymous@bryzek.com"

  lazy val systemUser: User = {
    findAll(email = Some(SystemEmailAddress), limit = 1).headOption.getOrElse {
      sys.error(s"Could not find system user[$SystemEmailAddress]")
    }
  }

  lazy val anonymousUser: User = {
    findAll(email = Some(AnonymousEmailAddress), limit = 1).headOption.getOrElse {
      sys.error(s"Could not find anonymous user[$AnonymousEmailAddress]")
    }
  }

  private[this] val BaseQuery = s"""
    select users.guid,
           users.email,
           users.first_name as users_name_first,
           users.last_name as users_name_last,
           users.avatar_url,
           ${AuditsDao.all("users")}
      from users
     where true
  """

  private[this] val InsertQuery = """
    insert into users
    (guid, email, first_name, last_name, avatar_url, updated_by_guid, created_by_guid)
    values
    ({guid}::uuid, {email}, {first_name}, {last_name}, {avatar_url}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val InsertExternalIdQuery = """
    insert into user_external_ids
    (guid, user_guid, system, id, updated_by_guid, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {system}, {id}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """
  
  

  def validate(form: UserForm): Seq[String] = {
    form.email match {
      case None => {
        Seq("Please provide an email address")
      }
      case Some(email) => {
        if (email.trim == "") {
          Seq("Email address cannot be empty")

        } else if (!isValidEmail(email)) {
          Seq("Please enter a valid email address")

        } else {
          UsersDao.findByEmail(email) match {
            case None => Nil
            case Some(_) => Seq("Email is already registered")
          }
        }
      }
    }
  }

  private def isValidEmail(email: String): Boolean = {
    email.indexOf("@") >= 0
  }

  def create(createdBy: Option[User], form: UserForm): Either[Seq[String], User] = {
    validate(form) match {
      case Nil => {
        val userGuid = UUID.randomUUID

        DB.withTransaction { implicit c =>
          SQL(InsertQuery).on(
            'guid -> userGuid,
            'email -> form.email.getOrElse("").trim,
            'avatar_url -> Util.trimmedString(form.avatarUrl),
            'first_name -> Util.trimmedString(form.name.flatMap(_.first)),
            'last_name -> Util.trimmedString(form.name.flatMap(_.last)),
            'created_by_guid -> createdBy.getOrElse(UsersDao.anonymousUser).guid
          ).execute()

          form.externalIds.getOrElse(Nil).foreach { ext =>
            SQL(InsertExternalIdQuery).on(
              'guid -> UUID.randomUUID,
              'user_guid -> userGuid,
              'system -> ext.system.toString,
              'id -> ext.id.trim
            )
          }
        }

        Right(
          findByGuid(userGuid).getOrElse {
            sys.error("Failed to create user")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def findByEmail(email: String): Option[User] = {
    findAll(email = Some(email), limit = 1).headOption
  }

  def findByGuid(guid: UUID): Option[User] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    email: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy.asc("users", "created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[User] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and users.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("users.guid", _) },
      email.map { v => "and lower(users.email) = lower(trim({email}))" },
      isDeleted.map(Filters.isDeleted("users", _)),
      Some(s"order by $orderBy limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      email.map('email -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        io.flow.user.v0.anorm.parsers.User.table("users").*
      )
    }
  }

}


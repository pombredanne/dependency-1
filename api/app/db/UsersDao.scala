package db

import com.bryzek.dependency.actors.MainActor
import io.flow.play.postgresql.{AuditsDao, Filters}
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

  private[this] val BaseQuery = Query(s"""
    select users.guid,
           users.email,
           users.first_name as users_name_first,
           users.last_name as users_name_last,
           users.avatar_url,
           ${AuditsDao.all("users")}
      from users
  """)

  private[this] val InsertQuery = """
    insert into users
    (guid, email, first_name, last_name, avatar_url, updated_by_guid, created_by_guid)
    values
    ({guid}::uuid, {email}, {first_name}, {last_name}, {avatar_url}, {created_by_guid}::uuid, {created_by_guid}::uuid)
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
        val guid = UUID.randomUUID

        DB.withTransaction { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'email -> form.email.getOrElse("").trim,
            'avatar_url -> Util.trimmedString(form.avatarUrl),
            'first_name -> Util.trimmedString(form.name.flatMap(_.first)),
            'last_name -> Util.trimmedString(form.name.flatMap(_.last)),
            'created_by_guid -> createdBy.getOrElse(UsersDao.anonymousUser).guid
          ).execute()

          // TODO: Git hub user
        }

        MainActor.ref ! MainActor.Messages.UserCreated(guid)

        Right(
          findByGuid(guid).getOrElse {
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
    orderBy: OrderBy = OrderBy.parseOrError("users.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[User] = {
    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "users",
        auth = Clause.True, // TODO
        guid = guid,
        guids = guids,
        orderBy = orderBy,
        isDeleted = isDeleted,
        limit = Some(limit),
        offset = offset
      ).
        text(
          "users.email",
          email,
          columnFunctions = Seq(Query.Function.Lower),
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        as(
          io.flow.user.v0.anorm.parsers.User.table("users").*
        )
    }
  }

}


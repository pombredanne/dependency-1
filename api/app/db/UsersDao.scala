package db

import com.bryzek.dependency.actors.MainActor
import io.flow.postgresql.{Query, OrderBy}
import io.flow.user.v0.models.{Name, User, UserForm}
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
    select users.id,
           users.email,
           users.first_name as users_name_first,
           users.last_name as users_name_last,
           users.avatar_url
      from users
  """)

  private[this] val InsertQuery = """
    insert into users
    (id, email, first_name, last_name, avatar_url, updated_by_user_id)
    values
    ({id}, {email}, {first_name}, {last_name}, {avatar_url}, {updated_by_user_id})
  """

  def validate(form: UserForm): Seq[String] = {
    form.email match {
      case None => {
        Nil
      }
      case Some(email) => {
        if (email.trim.isEmpty) {
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
        val id = io.flow.play.util.IdGenerator("usr").randomId()

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'id -> id,
            'email -> form.email.map(_.trim),
            'avatar_url -> Util.trimmedString(form.avatarUrl),
            'first_name -> Util.trimmedString(form.name.flatMap(_.first)),
            'last_name -> Util.trimmedString(form.name.flatMap(_.last)),
            'updated_by_user_id -> createdBy.getOrElse(UsersDao.anonymousUser).id
          ).execute()
        }

        MainActor.ref ! MainActor.Messages.UserCreated(id.toString)

        Right(
          findById(id).getOrElse {
            sys.error("Failed to create user")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def findByGithubUserId(githubUserId: Long): Option[User] = {
    findAll(githubUserId = Some(githubUserId), limit = 1).headOption
  }

  def findByEmail(email: String): Option[User] = {
    findAll(email = Some(email), limit = 1).headOption
  }

  def findById(id: String): Option[User] = {
    findAll(id = Some(id), limit = 1).headOption
  }

  def findAll(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    email: Option[String] = None,
    identifier: Option[String] = None,
    githubUserId: Option[Long] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("users.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[User] = {
    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "users",
        auth = Clause.True, // TODO
        id = id,
        ids = ids,
        orderBy = orderBy.sql,
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
        subquery("users.id", "identifier", identifier, { bindVar =>
          s"select user_id from user_identifiers where deleted_at is null and value = trim(${bindVar.sql})"
        }).
        subquery("users.id", "github_user_id", githubUserId, { bindVar =>
          s"select user_id from github_users where deleted_at is null and id = ${bindVar.sql}"
        }).
        as(
          io.flow.user.v0.anorm.parsers.User.table("users").*
        )
    }
  }

}


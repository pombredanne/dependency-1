package db

import com.bryzek.dependency.v0.models.{Name, User, UserForm}
import io.flow.common.v0.models.Audit
import io.flow.play.postgresql.{AuditsDao, Filters, OrderBy}
import io.flow.play.util.ValidatedForm
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
           users.first_name,
           users.last_name,
           ${AuditsDao.queryCreation("users")}
      from users
     where true
  """

  private[this] val InsertQuery = """
    insert into users
    (guid, email, first_name, last_name, updated_by_guid, created_by_guid)
    values
    ({guid}::uuid, {email}, {first_name}, {last_name}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def validate(form: UserForm): ValidatedForm[UserForm] = {
    val emailErrors = if (form.email.trim == "") {
      Seq("Email address cannot be empty")

    } else if (!isValidEmail(form.email)) {
      Seq("Please enter a valid email address")

    } else {
      UsersDao.findByEmail(form.email) match {
        case None => Nil
        case Some(_) => Seq("Email is already registered")
      }
    }

    ValidatedForm(form, emailErrors)
  }

  private def isValidEmail(email: String): Boolean = {
    email.indexOf("@") >= 0
  }

  def create(createdBy: Option[User], valid: ValidatedForm[UserForm]): User = {
    valid.assertValid()

    val userGuid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> userGuid,
        'email -> valid.form.email.trim,
        'first_name -> Util.trimmedString(valid.form.name.flatMap(_.first)),
        'last_name -> Util.trimmedString(valid.form.name.flatMap(_.last)),
        'created_by_guid -> createdBy.getOrElse(UsersDao.anonymousUser).guid
      ).execute()
    }

    findByGuid(userGuid).getOrElse {
      sys.error("Failed to create user")
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
      email.map { v => "and users.guid = (select user_guid from emails where lower(email) = lower(trim({email})) and deleted_at is null)" },
      isDeleted.map(Filters.isDeleted("users", _)),
      Some(s"order by $orderBy limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      email.map('email -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): User = {
    User(
      guid = row[UUID]("guid"),
      email = row[String]("email"),
      name = nameFromRow(row),
      audit = AuditsDao.fromRowCreation(row)
    )
  }

  private[db] def nameFromRow(
    row: anorm.Row
  ): Option[Name] = {
    val name = Name(
      first = row[Option[String]]("first_name"),
      last = row[Option[String]]("last_name")
    )
    (name.first, name.last) match {
      case (None, None) => None
      case _ => Some(name)
    }
  }

}


package db

import com.bryzek.dependency.v0.models.Reference
import io.flow.user.v0.models.User
import io.flow.postgresql.{Query, OrderBy}
import com.bryzek.dependency.v0.models.Publication
import org.joda.time.DateTime

import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

case class LastEmailForm(
  userId: String,
  publication: Publication
)

case class LastEmail(
  id: String,
  user: Reference,
  publication: Publication,
  createdAt: DateTime
)

object LastEmailsDao {

  private[this] val BaseQuery = Query(s"""
    select last_emails.*
      from last_emails
  """)

  private[this] val InsertQuery = """
    insert into last_emails
    (id, user_id, publication, created_by_id, updated_by_id)
    values
    ({id}, {user_id}, {publication}, {updated_by_user_id})
  """

  def record(
    createdBy: User,
    form: LastEmailForm
  ): LastEmail = {
    val id = DB.withTransaction { implicit c =>
      findByUserIdAndPublication(form.userId, form.publication).foreach { rec =>
        SoftDelete.delete(c, "last_emails", createdBy.id, rec.id)
      }
      create(createdBy, form)
    }
    findById(id).getOrElse {
      sys.error("Failed to record last email")
    }
  }

  def softDelete(deletedBy: User, rec: LastEmail) {
    SoftDelete.delete("last_emails", deletedBy.id, rec.id)
  }

  private[this] def create(
    createdBy: User,
    form: LastEmailForm
  ) (
    implicit c: java.sql.Connection
  ): String = {
    val id = io.flow.play.util.IdGenerator("lse").randomId()
    SQL(InsertQuery).on(
      'id -> id,
      'user_id -> form.userId,
      'publication -> form.publication.toString,
      'updated_by_user_id -> createdBy.id
    ).execute()
    id
  }

  def findById(id: String): Option[LastEmail] = {
    findAll(id = Some(id), limit = 1).headOption
  }

  def findByUserIdAndPublication(userId: String, publication: Publication): Option[LastEmail] = {
    findAll(userId = Some(userId), publication = Some(publication), limit = 1).headOption
  }

  def findAll(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    userId: Option[String] = None,
    publication: Option[Publication] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("-last_emails.publication, last_emails.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[LastEmail] = {

    DB.withConnection { implicit c =>
      BaseQuery.
        equals("last_emails.id", id).
        in("last_emails.id", ids).
        equals("last_emails.user_id", userId).
        text("last_emails.publication", publication).
        nullBoolean("last_emails.deleted_at", isDeleted).
        orderBy(orderBy.sql).
        limit(Some(limit)).
        offset(Some(offset)).
        as(parser.*)
    }
  }

  private[this] val parser: RowParser[LastEmail] = {
    SqlParser.str("id") ~
    SqlParser.str("user_id") ~
    com.bryzek.dependency.v0.anorm.parsers.Publication.parser(
      com.bryzek.dependency.v0.anorm.parsers.Publication.Mappings("publication")
    ) ~
    SqlParser.get[DateTime]("created_at") map {
      case id ~ userId ~ publication ~ createdAt => {
        LastEmail(
          id = id,
          user = Reference(id = userId),
          publication = publication,
          createdAt = createdAt
        )
      }
    }
  }


}

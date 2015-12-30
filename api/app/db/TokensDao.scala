package db

import com.bryzek.dependency.v0.models.{Token, TokenForm}
import io.flow.user.v0.models.User
import io.flow.postgresql.{Query, OrderBy}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

object TokensDao {

  val GithubOauthTokenTag = "github_oauth"

  private[this] val BaseQuery = Query(s"""
    select tokens.id,
           tokens.user_id as tokens_user_id,
           tokens.tag,
           tokens.token
      from tokens
  """)

  private[this] val InsertQuery = """
    insert into tokens
    (id, user_id, tag, token, updated_by_user_id
    values
    ({id}, {user_id}, {tag}, {token}, {updated_by_user_id})
  """

  def upsert(createdBy: User, form: TokenForm): Token = {
    findByUserIdAndTag(form.userId, form.tag) match {
      case None => {
        create(createdBy, form)
      }
      case Some(existing) => {
        (existing.token == form.token) match {
          case true => existing
          case false => {
            DB.withTransaction { implicit c =>
              SoftDelete.delete(c, "tokens", createdBy.id, existing.id)
              createWithConnection(createdBy, form)
            }
          }
        }
      }
    }
  }

  def create(createdBy: User, form: TokenForm): Token = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, form)
    }
  }

  private[this] def createWithConnection(createdBy: User, form: TokenForm)(implicit c: java.sql.Connection): Token = {
    val id = io.flow.play.util.IdGenerator("tok").randomId()

    SQL(InsertQuery).on(
      'id -> id,
      'user_id -> form.userId,
      'tag -> form.tag.trim,
      'token -> form.token.trim,
      'updated_by_user_id -> createdBy.id
    ).execute()

    findAllWithConnection(id = Some(id), limit = 1).headOption.getOrElse {
      sys.error("Failed to create token")
    }
  }

  def softDelete(deletedBy: User, token: Token) {
    SoftDelete.delete("tokens", deletedBy.id, token.id)
  }

  def findByUserIdAndTag(
    userId: String,
    tag: String
  ): Option[Token] = {
    findAll(
      userId = Some(userId),
      tag = Some(tag),
      limit = 1
    ).headOption
  }

  def findById(id: String): Option[Token] = {
    findAll(id = Some(id), limit = 1).headOption
  }

  def findAll(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    userId: Option[String] = None,
    tag: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Token] = {
    DB.withConnection { implicit c =>
      findAllWithConnection(
        id = id,
        ids = ids,
        userId = userId,
        tag = tag,
        isDeleted = isDeleted,
        limit = limit,
        offset = offset
      )
    }
  }

  private[this] def findAllWithConnection(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    userId: Option[String] = None,
    tag: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("tokens.created_at"),
    limit: Long = 25,
    offset: Long = 0
  )(implicit c: java.sql.Connection): Seq[Token] = {
    Standards.query(
      BaseQuery,
      tableName = "tokens",
      auth = Clause.True, // TODO
      id = id,
      ids = ids,
      orderBy = orderBy.sql,
      isDeleted = isDeleted,
      limit = Some(limit),
      offset = offset
    ).
      equals("tokens.user_id", userId).
      text("tokens.tag", tag, valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)).
      as(
        com.bryzek.dependency.v0.anorm.parsers.Token.table("tokens").*
      )
  }

}

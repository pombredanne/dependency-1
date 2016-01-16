package db

import com.bryzek.dependency.v0.models.{Token, TokenForm}
import io.flow.common.v0.models.User
import io.flow.play.util.Random
import io.flow.postgresql.{Query, OrderBy}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

sealed trait InternalTokenForm {

  def userId: String
  def tag: String
  def token: String
  def description: Option[String]

}

object InternalTokenForm {

  private[db] val GithubOauthTag = "github_oauth"
  private[db] val UserCreatedTag = "user_created"

  private[this] val random = Random()
  private[this] val TokenLength = 64

  case class GithubOauth(userId: String, token: String) extends InternalTokenForm {
    override val tag = GithubOauthTag
    override val description = None
  }

  case class UserCreated(form: TokenForm) extends InternalTokenForm {
    override val tag = UserCreatedTag
    override val userId = form.userId
    override val description = form.description
    def token = random.alphaNumeric(TokenLength)
  }

}


object TokensDao {

  private[this] val BaseQuery = Query(s"""
    select tokens.id,
           tokens.user_id,
           tokens.tag,
           substr(token, 1, 3) || '-masked-xxx' as masked,
           null as cleartext,
           tokens.description
      from tokens
  """)

  private[this] val SelectCleartextTokenQuery = Query(s"""
    select token, number_views from tokens
  """)

  private[this] val InsertQuery = """
    insert into tokens
    (id, user_id, tag, token, description, updated_by_user_id)
    values
    ({id}, {user_id}, {tag}, {token}, {description}, {updated_by_user_id})
  """

  private[this] val IncrementNumberViewsQuery = """
    update tokens set number_views = number_views + 1, updated_by_user_id = {updated_by_user_id} where id = {id}
  """

  def setLatestByTag(createdBy: User, form: InternalTokenForm): Token = {
    findAll(Authorization.All, userId = Some(form.userId), tag = Some(form.tag), limit = 1).headOption match {
      case None => {
        create(createdBy, form) match {
          case Left(errors) => sys.error("Failed to create token: " + errors.mkString(", "))
          case Right(token) => token
        }
      }
      case Some(existing) => {
        DB.withTransaction { implicit c =>
          SoftDelete.delete(c, "tokens", createdBy.id, existing.id)
          createWithConnection(createdBy, form) match {
            case Left(errors) => sys.error("Failed to create token: " + errors.mkString(", "))
            case Right(token) => token
          }
        }
      }
    }
  }

  def create(createdBy: User, form: InternalTokenForm): Either[Seq[String], Token] = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, form)
    }
  }

  private[db] def validate(
    form: InternalTokenForm
  ): Seq[String] = {
    form match {
      case InternalTokenForm.GithubOauth(userId, token) => Nil
      case InternalTokenForm.UserCreated(f) => {
        UsersDao.findById(f.userId) match {
          case None => Seq("User not found")
          case Some(_) => Nil
        }
      }
    }
  }

  private[this] def createWithConnection(createdBy: User, form: InternalTokenForm)(implicit c: java.sql.Connection): Either[Seq[String], Token] = {
    validate(form) match {
      case Nil => {
        val id = io.flow.play.util.IdGenerator("tok").randomId()

        SQL(InsertQuery).on(
          'id -> id,
          'user_id -> form.userId,
          'tag -> form.tag.trim,
          'token -> form.token.trim,
          'description -> Util.trimmedString(form.description),
          'updated_by_user_id -> createdBy.id
        ).execute()

        Right(
          findAllWithConnection(Authorization.All, id = Some(id), limit = 1).headOption.getOrElse {
            sys.error("Failed to create token")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def addCleartextIfAvailable(user: User, token: Token): Token = {
    DB.withConnection { implicit c =>
      SelectCleartextTokenQuery.equals("tokens.id", Some(token.id)).as(
        cleartextTokenParser().*
      ).headOption match {
        case None => token
        case Some(clear) => {
          clear.numberViews match {
            case 0 => {
              incrementNumberViews(user, token.id)(c)
              token.copy(cleartext = Some(clear.token))
            }
            case _ => {
              token
            }
          }
        }
      }
    }
  }

  private[this] def incrementNumberViews(createdBy: User, tokenId: String)(
    implicit c: java.sql.Connection
  ) {
    SQL(IncrementNumberViewsQuery).on(
      'id -> tokenId,
      'updated_by_user_id -> createdBy.id
    ).execute()
  }

  def softDelete(deletedBy: User, token: Token) {
    SoftDelete.delete("tokens", deletedBy.id, token.id)
  }

  def getCleartextGithubOauthTokenByUserId(
    userId: String
  ): Option[String] = {
    DB.withConnection { implicit c =>
      SelectCleartextTokenQuery.
        isNull("tokens.deleted_at").
        equals("tokens.user_id", Some(userId)).
        text("tokens.tag", Some(InternalTokenForm.GithubOauthTag)).
        limit(Some(1)).
        orderBy(Some("tokens.created_at desc")).
        as(
          cleartextTokenParser().*
        ).headOption.map(_.token)
    }
  }

  private[this] case class CleartextToken(token: String, numberViews: Long)

  private[this] def cleartextTokenParser() = {
    SqlParser.str("token") ~ SqlParser.long("number_views") map { case token ~ numberViews => CleartextToken(token, numberViews) }
  }

  def findById(auth: Authorization, id: String): Option[Token] = {
    findAll(auth, id = Some(id), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    userId: Option[String] = None,
    tag: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("tokens.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Token] = {
    DB.withConnection { implicit c =>
      findAllWithConnection(
        auth,
        id = id,
        ids = ids,
        userId = userId,
        tag = tag,
        isDeleted = isDeleted,
        limit = limit,
        offset = offset,
        orderBy = orderBy
      )
    }
  }

  private[this] def findAllWithConnection(
    auth: Authorization,
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
      auth = auth.users("tokens.user_id"),
      id = id,
      ids = ids,
      orderBy = orderBy.sql,
      isDeleted = isDeleted,
      limit = Some(limit),
      offset = offset
    ).
      equals("tokens.user_id", userId).
      text("tokens.tag", tag, valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)).
      orderBy(orderBy.sql).
      as(
        com.bryzek.dependency.v0.anorm.parsers.Token.parser().*
      )
  }

}

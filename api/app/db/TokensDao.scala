package db

import com.bryzek.dependency.v0.models.{Token, TokenForm}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object TokensDao {

  val GithubOauthTokenTag = "github_oauth"

  private[this] val BaseQuery = Query(s"""
    select tokens.guid,
           tokens.user_guid as tokens_user_guid,
           tokens.tag,
           tokens.token,
           ${AuditsDao.all("tokens")}
      from tokens
  """)

  private[this] val InsertQuery = """
    insert into tokens
    (guid, user_guid, tag, token, updated_by_guid, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {tag}, {token}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, form: TokenForm): Token = {
    findByUserGuidAndTag(form.userGuid, form.tag) match {
      case None => {
        create(createdBy, form)
      }
      case Some(existing) => {
        (existing.token == form.token) match {
          case true => existing
          case false => {
            DB.withTransaction { implicit c =>
              SoftDelete.delete(c, "tokens", createdBy.guid, existing.guid)
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
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'user_guid -> form.userGuid,
      'tag -> form.tag.trim,
      'token -> form.token.trim,
      'created_by_guid -> createdBy.guid
    ).execute()

    findAllWithConnection(guid = Some(guid), limit = 1).headOption.getOrElse {
      sys.error("Failed to create token")
    }
  }

  def softDelete(deletedBy: User, token: Token) {
    SoftDelete.delete("tokens", deletedBy.guid, token.guid)
  }

  def findByUserGuidAndTag(
    userGuid: UUID,
    tag: String
  ): Option[Token] = {
    findAll(
      userGuid = Some(userGuid),
      tag = Some(tag),
      limit = 1
    ).headOption
  }

  def findByGuid(guid: UUID): Option[Token] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
    tag: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Token] = {
    DB.withConnection { implicit c =>
      findAllWithConnection(
        guid = guid,
        guids = guids,
        userGuid = userGuid,
        tag = tag,
        isDeleted = isDeleted,
        limit = limit,
        offset = offset
      )
    }
  }

  private[this] def findAllWithConnection(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
    tag: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy.parseOrError("tokens.created_at"),
    limit: Long = 25,
    offset: Long = 0
  )(implicit c: java.sql.Connection): Seq[Token] = {
    Standards.query(
      BaseQuery,
      tableName = "tokens",
      auth = Clause.True, // TODO
      guid = guid,
      guids = guids,
      orderBy = orderBy,
      isDeleted = isDeleted,
      limit = Some(limit),
      offset = offset
    ).
      uuid("tokens.user_guid", userGuid).
      text("tokens.tag", tag, valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)).
      as(
        com.bryzek.dependency.v0.anorm.parsers.Token.table("tokens").*
      )
  }

}

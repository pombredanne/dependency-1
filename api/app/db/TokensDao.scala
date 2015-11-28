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

  private[this] val BaseQuery = s"""
    select tokens.guid,
           tokens.user_guid as tokens_user_guid,
           tokens.tag,
           tokens.token,
           ${AuditsDao.all("tokens")}
      from tokens
     where true
  """

  private[this] val InsertQuery = """
    insert into tokens
    (guid, user_guid, tag, token, updated_by_guid, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {tag}, {token}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, form: TokenForm): Token = {
    findByUserGuidAndTagAndToken(form.userGuid, form.tag, form.token).getOrElse {
      create(createdBy, form)
    }
  }

  def create(createdBy: User, form: TokenForm): Token = {
    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'user_guid -> form.userGuid,
        'tag -> form.tag.trim,
        'token -> form.token.trim,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create token")
    }
  }

  def softDelete(deletedBy: User, token: Token) {
    SoftDelete.delete("tokens", deletedBy.guid, token.guid)
  }

  def findByUserGuidAndTagAndToken(
    userGuid: UUID,
    tag: String,
    token: String
  ): Option[Token] = {
    findAll(
      userGuid = Some(userGuid),
      tag = Some(tag),
      token = Some(token),
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
    token: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Token] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v =>  "and tokens.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("tokens.guid", _) },
      userGuid.map { v => "and tokens.user_guid = {user_guid}::uuid" },
      tag.map { v => "and tokens.tag = lower(trim({tag}))" },
      token.map { v => "and tokens.token = trim({token})" },
      isDeleted.map(Filters.isDeleted("tokens", _)),
      Some(s"order by tokens.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      tag.map('tag -> _.toString),
      token.map('token -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Token.table("tokens").*
      )
    }
  }

}

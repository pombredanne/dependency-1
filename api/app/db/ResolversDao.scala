package db

import com.bryzek.dependency.v0.models.{Resolver, ResolverForm, Visibility}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object ResolversDao {

  val GithubOauthResolverTag = "github_oauth"

  private[this] val BaseQuery = s"""
    select resolvers.guid,
           resolvers.visibility,
           resolvers.user_guid as resolvers_user_guid,
           resolvers.uri,
           resolvers.position,
           ${AuditsDao.all("resolvers")}
      from resolvers
     where true
  """

  private[this] val InsertQuery = """
    insert into resolvers
    (guid, visibility, position, user_guid, uri, updated_by_guid, created_by_guid)
    values
    ({guid}::uuid, {visibility}, {position}, {user_guid}::uuid, {uri}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, form: ResolverForm): Resolver = {
    findByUserGuidAndUri(form.userGuid, form.uri).getOrElse {
      create(createdBy, form)
    }
  }

  def create(createdBy: User, form: ResolverForm): Resolver = {
    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'user_guid -> form.userGuid,
        'visibility -> form.visibility.toString,
        'position -> nextPosition(form.userGuid, form.visibility),
        'uri -> form.uri.trim,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create resolver")
    }
  }

  def softDelete(deletedBy: User, resolver: Resolver) {
    SoftDelete.delete("resolvers", deletedBy.guid, resolver.guid)
  }

  def findByUserGuidAndUri(
    userGuid: UUID,
    uri: String
  ): Option[Resolver] = {
    findAll(
      userGuid = Some(userGuid),
      uri = Some(uri),
      limit = 1
    ).headOption
  }

  def findByGuid(guid: UUID): Option[Resolver] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
    uri: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Resolver] = {
      val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v =>  "and resolvers.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("resolvers.guid", _) },
      userGuid.map { v => "and resolvers.user_guid = {user_guid}::uuid" },
      uri.map { v => "and resolvers.uri = trim({uri})" },
      isDeleted.map(Filters.isDeleted("resolvers", _)),
      Some(s"order by lower(resolvers.uri), resolvers.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      uri.map('uri -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Resolver.table("resolvers").*
      )
    }
  }

  private[this] val NextPublicPositionQuery = """
    select coalesce(max(position) + 1, 0) as position
      from resolvers
     where visibility = 'public'
       and deleted_at is null
  """

  private[this] val NextPrivatePositionQuery = """
    select coalesce(max(position) + 1, 0) as position
      from resolvers
     where visibility = 'private'
       and user_guid = {user_guid}::uuid
       and deleted_at is null
  """

  /**
    * Returns the next free position
    */
  def nextPosition(
    userGuid: UUID,
    visibility: Visibility
  ): Int = {
    DB.withConnection { implicit c =>    
      visibility match {
        case Visibility.Public => {
          SQL(NextPublicPositionQuery).as(SqlParser.int("position").single)
        }
        case  Visibility.Private | Visibility.UNDEFINED(_) => {
          SQL(NextPrivatePositionQuery).on("user_guid" -> userGuid.toString).as(SqlParser.int("position").single)
        }
      }
    }
  }

}

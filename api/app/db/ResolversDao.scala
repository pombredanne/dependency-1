package db

import com.bryzek.dependency.api.lib.Validation
import com.bryzek.dependency.v0.models.{Credentials, CredentialsUndefinedType, Resolver, ResolverForm, UsernamePassword, Visibility}
import com.bryzek.dependency.v0.models.json._
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
           resolvers.credentials,
           resolvers.uri,
           resolvers.position,
           ${AuditsDao.all("resolvers")},
           organizations.guid as resolvers_organization_guid,
           organizations.key as resolvers_organization_key
      from resolvers
      left join organizations on organizations.deleted_at is null and organizations.guid = resolvers.organization_guid
     where true
  """

  private[this] val SelectCredentialsQuery = s"""
    select credentials from resolvers where guid = {guid}::uuid
  """

  private[this] val InsertQuery = """
    insert into resolvers
    (guid, visibility, credentials, position, organization_guid, uri, updated_by_guid, created_by_guid)
    values
    ({guid}::uuid, {visibility}, {credentials}::json, {position}, {organization_guid}::uuid, {uri}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def credentials(resolver: Resolver): Option[Credentials] = {
    resolver.credentials.flatMap { _ =>
      import com.bryzek.dependency.v0.anorm.conversions.Json._
      DB.withConnection { implicit c =>
        SQL(SelectCredentialsQuery).on('guid -> resolver.guid.toString).as(
          SqlParser.get[Credentials]("credentials").*
        ).headOption
      }
    }
  }

  def validate(user: User, form: ResolverForm): Seq[String] = {
    val urlErrors = Validation.validateUri(form.uri) match {
      case Left(errors) => errors
      case Right(url) => Nil
    }

    val uniqueErrors = form.visibility match {
      case Visibility.Public | Visibility.UNDEFINED(_) => {
        findAll(
          Authorization.All,
          visibility = Some(Visibility.Public),
          uri = Some(form.uri),
          limit = 1
        ).headOption match {
          case None => Nil
          case Some(_) => Seq(s"Public resolver with uri[${form.uri}] already exists")
        }
      }
      case Visibility.Private => {
        findAll(
          Authorization.All,
          visibility = Some(Visibility.Private),
          organizationGuid = Some(form.organizationGuid),
          uri = Some(form.uri),
          limit = 1
        ).headOption match {
          case None => Nil
          case Some(_) => Seq(s"Organization already has a resolver with uri[${form.uri}]")
        }
      }
    }

    val organizationErrors = MembershipsDao.isMember(form.organizationGuid, user) match  {
      case false => Seq("You do not have access to this organization")
      case true => Nil
    }

    urlErrors ++ uniqueErrors ++ organizationErrors
  }

  def upsert(createdBy: User, form: ResolverForm): Either[Seq[String], Resolver] = {
    findByOrganizationGuidAndUri(Authorization.All, form.organizationGuid, form.uri) match {
      case Some(resolver) => Right(resolver)
      case None => create(createdBy, form)
    }
  }

  def create(createdBy: User, form: ResolverForm): Either[Seq[String], Resolver] = {
    validate(createdBy, form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'organization_guid -> form.organizationGuid,
            'visibility -> form.visibility.toString,
            'credentials -> form.credentials.map { cred => Json.stringify(Json.toJson(cred)) },
            'position -> nextPosition(form.organizationGuid, form.visibility),
            'uri -> form.uri.trim,
            'created_by_guid -> createdBy.guid
          ).execute()
        }

        Right(
          findByGuid(Authorization.All, guid).getOrElse {
            sys.error("Failed to create resolver")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, resolver: Resolver) {
    SoftDelete.delete("resolvers", deletedBy.guid, resolver.guid)
  }

  def findByOrganizationGuidAndUri(
    auth: Authorization,
    organizationGuid: UUID,
    uri: String
  ): Option[Resolver] = {
    findAll(
      auth,
      organizationGuid = Some(organizationGuid),
      uri = Some(uri),
      limit = 1
    ).headOption
  }

  def findByGuid(auth: Authorization, guid: UUID): Option[Resolver] = {
    findAll(auth, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    visibility: Option[Visibility] = None,
    org: Option[String] = None,
    organizationGuid: Option[UUID] = None,
    uri: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Resolver] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      Some(auth.organizations("resolvers.organization_guid", Some("resolvers.visibility")).and),
      guid.map { v =>  "and resolvers.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("resolvers.guid", _) },
      visibility.map { v => "and resolvers.visibility = {visibility}" },
      org.map { LocalFilters.organizationByKey("organizations.key", "org_key", _) },
      organizationGuid.map { v => "and resolvers.organization_guid = {organization_guid}::uuid" },
      uri.map { v => "and resolvers.uri = trim({uri})" },
      isDeleted.map(Filters.isDeleted("resolvers", _)),
        Some(s"""
          order by case when visibility = '${Visibility.Public}' then 0
                        when visibility = '${Visibility.Private}' then 1
                        else 2 end, resolvers.position, lower(resolvers.uri), resolvers.created_at
          limit ${limit} offset ${offset}
        """.trim)
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      organizationGuid.map('organization_guid -> _.toString),
      visibility.map('visibility -> _.toString),
      org.map('org_key -> _),
      uri.map('uri -> _.toString),
      Some('public -> Visibility.Public.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Resolver.table("resolvers").*
      ).map { maskCredentials(_) }
    }
  }

  /**
    * If this resolver has credentials, masks any passwords, returning
    * the resulting resolver.
    */
  def maskCredentials(resolver: Resolver): Resolver = {
    resolver.credentials match {
      case None => {
        resolver
      }
      case Some(cred) => {
        resolver.copy(
          credentials = Util.maskCredentials(cred)
        )
      }
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
       and organization_guid = {organization_guid}::uuid
       and deleted_at is null
  """

  /**
    * Returns the next free position
    */
  def nextPosition(
    organizationGuid: UUID,
    visibility: Visibility
  ): Int = {
    DB.withConnection { implicit c =>    
      visibility match {
        case Visibility.Public => {
          SQL(NextPublicPositionQuery).as(SqlParser.int("position").single)
        }
        case  Visibility.Private | Visibility.UNDEFINED(_) => {
          SQL(NextPrivatePositionQuery).on("organization_guid" -> organizationGuid.toString).as(SqlParser.int("position").single)
        }
      }
    }
  }

}

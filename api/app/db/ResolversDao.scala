package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.api.lib.Validation
import com.bryzek.dependency.v0.models.{Credentials, CredentialsUndefinedType, Resolver, ResolverForm, ResolverSummary}
import com.bryzek.dependency.v0.models.{OrganizationSummary, UsernamePassword, Visibility}
import com.bryzek.dependency.v0.models.json._
import io.flow.common.v0.models.User
import io.flow.postgresql.{Query, OrderBy, Pager}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

object ResolversDao {

  val GithubOauthResolverTag = "github_oauth"

  private[this] val BaseQuery = Query(s"""
    select resolvers.id,
           resolvers.visibility,
           resolvers.credentials,
           resolvers.uri,
           resolvers.position,
           organizations.id as organization_id,
           organizations.key as organization_key
      from resolvers
      left join organizations on organizations.deleted_at is null and organizations.id = resolvers.organization_id
  """)

  private[this] val SelectCredentialsQuery = s"""
    select credentials from resolvers where id = {id}
  """

  private[this] val InsertQuery = """
    insert into resolvers
    (id, visibility, credentials, position, organization_id, uri, updated_by_user_id)
    values
    ({id}, {visibility}, {credentials}::json, {position}, {organization_id}, {uri}, {updated_by_user_id})
  """

  def credentials(resolver: Resolver): Option[Credentials] = {
    resolver.credentials.flatMap { _ =>
      import com.bryzek.dependency.v0.anorm.conversions.Json._
      DB.withConnection { implicit c =>
        SQL(SelectCredentialsQuery).on('id -> resolver.id.toString).as(
          SqlParser.get[JsObject]("credentials").*
        ).headOption.flatMap { js =>
          js.validate[Credentials] match {
            case JsSuccess(credentials, _) => Some(credentials)
            case JsError(error) => {
              play.api.Logger.warn(s"Resolver[${resolver.id}] has credentials that could not be parsed: $error")
              None
            }
          }
        }
      }
    }
  }

  def toSummary(resolver: Resolver): ResolverSummary = {
    ResolverSummary(
      id = resolver.id,
      organization = resolver.organization.map { org =>
        OrganizationSummary(org.id, org.key)
      },
      visibility = resolver.visibility,
      uri = resolver.uri
    )
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
          organization = Some(form.organization),
          uri = Some(form.uri),
          limit = 1
        ).headOption match {
          case None => Nil
          case Some(_) => Seq(s"Organization already has a resolver with uri[${form.uri}]")
        }
      }
    }

    val organizationErrors = MembershipsDao.isMemberByOrgKey(form.organization, user) match  {
      case false => Seq("You do not have access to this organization")
      case true => Nil
    }

    urlErrors ++ uniqueErrors ++ organizationErrors
  }

  def upsert(createdBy: User, form: ResolverForm): Either[Seq[String], Resolver] = {
    findByOrganizationAndUri(Authorization.All, form.organization, form.uri) match {
      case Some(resolver) => Right(resolver)
      case None => create(createdBy, form)
    }
  }

  def create(createdBy: User, form: ResolverForm): Either[Seq[String], Resolver] = {
    validate(createdBy, form) match {
      case Nil => {
        val org = OrganizationsDao.findByKey(Authorization.All, form.organization).getOrElse {
          sys.error("Could not find organization with key[${form.organization}]")
        }

        val id = io.flow.play.util.IdGenerator("res").randomId()

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'id -> id,
            'organization_id -> org.id,
            'visibility -> form.visibility.toString,
            'credentials -> form.credentials.map { cred => Json.stringify(Json.toJson(cred)) },
            'position -> nextPosition(org.id, form.visibility),
            'uri -> form.uri.trim,
            'updated_by_user_id -> createdBy.id
          ).execute()
        }

        MainActor.ref ! MainActor.Messages.ResolverCreated(id)

        Right(
          findById(Authorization.All, id).getOrElse {
            sys.error("Failed to create resolver")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, resolver: Resolver) {
    Pager.create { offset =>
      LibrariesDao.findAll(
        Authorization.All,
        resolverId = Some(resolver.id),
        offset = offset
      )
    }.foreach { library =>
      LibrariesDao.softDelete(MainActor.SystemUser, library)
    }

    MainActor.ref ! MainActor.Messages.ResolverDeleted(resolver.id)
    SoftDelete.delete("resolvers", deletedBy.id, resolver.id)
  }

  def findByOrganizationAndUri(
    auth: Authorization,
    organization: String,
    uri: String
  ): Option[Resolver] = {
    findAll(
      auth,
      organization = Some(organization),
      uri = Some(uri),
      limit = 1
    ).headOption
  }

  def findById(auth: Authorization, id: String): Option[Resolver] = {
    findAll(auth, id = Some(id), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    visibility: Option[Visibility] = None,
    organization: Option[String] = None,
    organizationId: Option[String] = None,
    uri: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Resolver] = {
    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "resolvers",
        auth = auth.organizations("resolvers.organization_id", Some("resolvers.visibility")),
        id = id,
        ids = ids,
        orderBy = Some(s"""
          case when visibility = '${Visibility.Public}' then 0
               when visibility = '${Visibility.Private}' then 1
               else 2 end,
          resolvers.position, lower(resolvers.uri),resolvers.created_at
        """),
        isDeleted = isDeleted,
        limit = Some(limit),
        offset = offset
      ).
        text("resolvers.visibility", visibility).
        text("organizations.key", organization.map(_.toLowerCase)).
        equals("organizations.id", organizationId).
        text("resolvers.uri", uri).
        as(
          com.bryzek.dependency.v0.anorm.parsers.Resolver.parser().*
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
       and organization_id = {organization_id}
       and deleted_at is null
  """

  /**
    * Returns the next free position
    */
  def nextPosition(
    organizationId: String,
    visibility: Visibility
  ): Int = {
    DB.withConnection { implicit c =>    
      visibility match {
        case Visibility.Public => {
          SQL(NextPublicPositionQuery).as(SqlParser.int("position").single)
        }
        case  Visibility.Private | Visibility.UNDEFINED(_) => {
          SQL(NextPrivatePositionQuery).on("organization_id" -> organizationId.toString).as(SqlParser.int("position").single)
        }
      }
    }
  }

}

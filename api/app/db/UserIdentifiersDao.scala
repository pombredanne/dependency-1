package db

import com.bryzek.dependency.v0.models.UserIdentifier
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Query, OrderBy, SoftDelete}
import io.flow.play.util.UrlKey
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object UserIdentifiersDao {

  val GithubOauthUserIdentifierValue = "github_oauth"

  private[this] val BaseQuery = Query(s"""
    select user_identifiers.guid,
           user_identifiers.user_guid as user_identifiers_user_guid,
           user_identifiers.value,
           ${AuditsDao.all("user_identifiers")}
      from user_identifiers
  """)

  private[this] val InsertQuery = """
    insert into user_identifiers
    (guid, user_guid, value, updated_by_guid, created_by_guid)
    values
    ({guid}::equals, {user_guid}::equals, {value}, {created_by_guid}::equals, {created_by_guid}::equals)
  """

  /**
    * Returns the latest identifier, creating if necessary
    */
  def latestForUser(createdBy: User, user: User): UserIdentifier = {
    findAll(Authorization.All, userGuid = Some(user.guid)).headOption match {
      case None => {
        createForUser(createdBy, user)
      }
      case Some(existing) => {
        existing
      }
    }
  }

  def createForUser(createdBy: User, user: User): UserIdentifier = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, user)
    }
  }

  private[this] val IdentifierLength = 60
  private[this] val random = new scala.util.Random
  private[this] val Characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
  private[this] val Numbers = "0123456789"
  private[this] val CharactersAndNumbers = Characters + Numbers

  private[this] def randomString(alphabet: String)(n: Int): String = {
    Stream.continually(random.nextInt(alphabet.size)).map(alphabet).take(n).mkString
  }

  /**
    * Generate a unique random string of length
    * IdentifierLength. Guaranteed to start with a letter (to avoid
    * any issues with leading zeroes)
    */
  private[this] def generateIdentifier(): String = {
    randomString(Characters)(1) +randomString(CharactersAndNumbers)(IdentifierLength - 1)
  }

  private[this] def createWithConnection(createdBy: User, user: User)(implicit c: java.sql.Connection): UserIdentifier = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'user_guid -> user.guid,
      'value -> generateIdentifier(),
      'created_by_guid -> createdBy.guid
    ).execute()

    findAllWithConnection(Authorization.All, guid = Some(guid), limit = 1).headOption.getOrElse {
      sys.error("Failed to create identifier")
    }
  }

  def softDelete(deletedBy: User, identifier: UserIdentifier) {
    SoftDelete.delete("user_identifiers", deletedBy.guid, identifier.guid)
  }

  def findByGuid(auth: Authorization, guid: UUID): Option[UserIdentifier] = {
    findAll(auth, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
    value: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[UserIdentifier] = {
    DB.withConnection { implicit c =>
      findAllWithConnection(
        auth,
        guid = guid,
        guids = guids,
        userGuid = userGuid,
        value = value,
        isDeleted = isDeleted,
        limit = limit,
        offset = offset
      )
    }
  }

  private[this] def findAllWithConnection(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
    value: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("-user_identifiers.created_at"),
    limit: Long = 25,
    offset: Long = 0
  )(implicit c: java.sql.Connection): Seq[UserIdentifier] = {
    Standards.query(
      BaseQuery,
      tableName = "user_identifiers",
      auth = Clause.True, // TODO. Right now we ignore auth as there is no way to filter to users
      guid = guid,
      guids = guids,
      orderBy = orderBy.sql,
      isDeleted = isDeleted,
      limit = Some(limit),
      offset = offset
    ).
      equals("user_identifiers.user_guid", userGuid).
      text("user_identifiers.value", value).
      as(
        com.bryzek.dependency.v0.anorm.parsers.UserIdentifier.table("user_identifiers").*
      )
  }

}

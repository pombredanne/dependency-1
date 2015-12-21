package db

import io.flow.play.postgresql.{AuditsDao, Filters}
import com.bryzek.dependency.v0.models.{GithubUser, GithubUserForm}
import io.flow.user.v0.models.User
import java.util.UUID
import anorm._
import play.api.db._
import play.api.Play.current

object GithubUsersDao {

  private[this] val BaseQuery = Query(s"""
    select github_users.guid,
           github_users.user_guid as github_users_user_guid,
           github_users.id,
           github_users.login,
           ${AuditsDao.all("github_users")}
      from github_users
  """)

  private[this] val InsertQuery = """
    insert into github_users
    (guid, user_guid, id, login, updated_by_guid, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {id}, {login}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsertByLogin(createdBy: Option[User], form: GithubUserForm): GithubUser = {
    DB.withConnection { implicit c =>
      upsertByLoginWithConnection(createdBy, form)
    }
  }

  def upsertByLoginWithConnection(createdBy: Option[User], form: GithubUserForm)(implicit c: java.sql.Connection): GithubUser = {
    findByLogin(form.login).getOrElse {
      createWithConnection(createdBy, form)
    }
  }

  def create(createdBy: Option[User], form: GithubUserForm): GithubUser = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, form)
    }
  }

  private[db] def createWithConnection(createdBy: Option[User], form: GithubUserForm)(implicit c: java.sql.Connection): GithubUser = {
    val guid = UUID.randomUUID
    SQL(InsertQuery).on(
      'guid -> guid,
      'user_guid -> form.userGuid,
      'id -> form.id,
      'login -> form.login.trim,
      'created_by_guid -> createdBy.getOrElse(UsersDao.anonymousUser).guid
    ).execute()

    findByGuid(guid).getOrElse {
      sys.error("Failed to create github user")
    }
  }

  def findByLogin(login: String): Option[GithubUser] = {
    findAll(login = Some(login), limit = 1).headOption
  }

  def findByGuid(guid: UUID): Option[GithubUser] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    userGuid: Option[UUID] = None,
    login: Option[String] = None,
    id: Option[Long] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy.parseOrError("github_users.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[GithubUser] = {
    DB.withConnection { implicit c =>
      BaseQuery.
        uuid("github_users.guid", guid).
        multi("github_users.guid", guids).
        text("github_users.login", login).
        number("github_users.login", id).
        nullBoolean("github_users.deleted_at", isDeleted).
        orderBy(orderBy.sql).
        limit(Some(limit)).
        offset(Some(offset)).
        as(
          com.bryzek.dependency.v0.anorm.parsers.GithubUser.table("github_users").*
        )
    }
  }

}


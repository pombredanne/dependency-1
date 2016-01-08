package db

import io.flow.postgresql.{Query, OrderBy}
import com.bryzek.dependency.v0.models.{GithubUser, GithubUserForm}
import io.flow.common.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current

object GithubUsersDao {

  private[this] val BaseQuery = Query(s"""
    select github_users.id,
           github_users.user_id as github_users_user_id,
           github_users.github_user_id,
           github_users.login
      from github_users
  """)

  private[this] val InsertQuery = """
    insert into github_users
    (id, user_id, github_user_id, login, updated_by_user_id)
    values
    ({id}, {user_id}, {github_user_id}, {login}, {updated_by_user_id})
  """

  def upsertById(createdBy: Option[User], form: GithubUserForm): GithubUser = {
    DB.withConnection { implicit c =>
      upsertByIdWithConnection(createdBy, form)
    }
  }

  def upsertByIdWithConnection(createdBy: Option[User], form: GithubUserForm)(implicit c: java.sql.Connection): GithubUser = {
    findByGithubUserId(form.githubUserId).getOrElse {
      createWithConnection(createdBy, form)
    }
  }

  def create(createdBy: Option[User], form: GithubUserForm): GithubUser = {
    DB.withConnection { implicit c =>
      createWithConnection(createdBy, form)
    }
  }

  private[db] def createWithConnection(createdBy: Option[User], form: GithubUserForm)(implicit c: java.sql.Connection): GithubUser = {
    val id = io.flow.play.util.IdGenerator("ghu").randomId()
    SQL(InsertQuery).on(
      'id -> id,
      'user_id -> form.userId,
      'github_user_id -> form.githubUserId,
      'login -> form.login.trim,
      'updated_by_user_id -> createdBy.getOrElse(UsersDao.anonymousUser).id
    ).execute()

    findById(id).getOrElse {
      sys.error("Failed to create github user")
    }
  }

  def findByGithubUserId(githubUserId: Long): Option[GithubUser] = {
    findAll(githubUserId = Some(githubUserId), limit = 1).headOption
  }

  def findById(id: String): Option[GithubUser] = {
    findAll(id = Some(Seq(id)), limit = 1).headOption
  }

  def findAll(
    id: Option[Seq[String]] = None,
    userId: Option[String] = None,
    login: Option[String] = None,
    githubUserId: Option[Long] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("github_users.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[GithubUser] = {
    DB.withConnection { implicit c =>
      BaseQuery.
        in("github_users.id", id).
        equals("github_users.user_id", userId).
        text("github_users.login", login).
        equals("github_users.github_user_id", githubUserId).
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


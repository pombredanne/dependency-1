package controllers

import db.{Authorization, ProjectsDao, ResolversDao}
import com.bryzek.dependency.v0.models.{Project, Resolver}
import io.flow.user.v0.models.User
import play.api.mvc.{Result, Results}
import java.util.UUID

trait Helpers {

  def withProject(guid: UUID)(
    f: Project => Result
  ): Result = {
    ProjectsDao.findByGuid(guid) match {
      case None => {
        Results.NotFound
      }
      case Some(project) => {
        f(project)
      }
    }
  }

  def withResolver(user: User, guid: UUID)(
    f: Resolver => Result
  ): Result = {
    ResolversDao.findByGuid(Authorization.User(user.guid), guid) match {
      case None => {
        Results.NotFound
      }
      case Some(resolver) => {
        f(resolver)
      }
    }
  }

}

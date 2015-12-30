package controllers

import db.{Authorization, OrganizationsDao, ProjectsDao, ResolversDao, UsersDao}
import com.bryzek.dependency.v0.models.{Organization, Project, Resolver}
import io.flow.user.v0.models.User
import play.api.mvc.{Result, Results}
import java.util.UUID

trait Helpers {

  def withUser(id: String)(
    f: User => Result
  ) = {
    UsersDao.findById(id) match {
      case None => {
        Results.NotFound
      }
      case Some(user) => {
        f(user)
      }
    }
  }

  def withOrganization(user: User, guid: UUID)(
    f: Organization => Result
  ) = {
    OrganizationsDao.findByGuid(Authorization.User(user.id), guid) match {
      case None => {
        Results.NotFound
      }
      case Some(organization) => {
        f(organization)
      }
    }
  }

  def withProject(user: User, guid: UUID)(
    f: Project => Result
  ): Result = {
    ProjectsDao.findByGuid(Authorization.User(user.id), guid) match {
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
    ResolversDao.findByGuid(Authorization.User(user.id), guid) match {
      case None => {
        Results.NotFound
      }
      case Some(resolver) => {
        f(resolver)
      }
    }
  }

}

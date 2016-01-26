package controllers

import db.{Authorization, BinariesDao, LibrariesDao, OrganizationsDao, ProjectsDao, ResolversDao, UsersDao}
import com.bryzek.dependency.v0.models.{Binary, Library, Organization, Project, Resolver}
import io.flow.common.v0.models.User
import play.api.mvc.{Result, Results}

trait Helpers {

  def withBinary(user: User, id: String)(
    f: Binary => Result
  ): Result = {
    BinariesDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(binary) => {
        f(binary)
      }
    }
  }
  
  def withLibrary(user: User, id: String)(
    f: Library => Result
  ): Result = {
    LibrariesDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(library) => {
        f(library)
      }
    }
  }

  def withOrganization(user: User, id: String)(
    f: Organization => Result
  ) = {
    OrganizationsDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(organization) => {
        f(organization)
      }
    }
  }

  def withProject(user: User, id: String)(
    f: Project => Result
  ): Result = {
    ProjectsDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(project) => {
        f(project)
      }
    }
  }

  def withResolver(user: User, id: String)(
    f: Resolver => Result
  ): Result = {
    ResolversDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(resolver) => {
        f(resolver)
      }
    }
  }

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

}

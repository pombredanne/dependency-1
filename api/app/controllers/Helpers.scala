package controllers

import db.ProjectsDao
import com.bryzek.dependency.v0.models.Project
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

}

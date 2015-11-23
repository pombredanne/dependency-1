package db

import com.bryzek.dependency.v0.models.{LibraryRecommendation, Project}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

object LibraryRecommendationsDao {

  def forProject(project: Project): Seq[LibraryRecommendation] = {
    Nil
  }

}

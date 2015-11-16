package lib

import com.bryzek.dependency.v0.models.User

sealed trait Section

object Section {
  case object Dashboard extends Section
  case object Projects extends Section
  case object Languages extends Section
  case object Libraries extends Section
}

case class UiData(
  requestPath: String,
  section: Option[Section] = None,
  title: Option[String] = None,
  headTitle: Option[String] = None,
  user: Option[User] = None,
  query: Option[String] = None
)

package lib

import com.bryzek.dependency.v0.models.User

case class UiData(
  requestPath: String,
  title: Option[String] = None,
  headTitle: Option[String] = None,
  user: Option[User] = None,
  query: Option[String] = None
)

package db

import java.util.UUID

sealed trait Authorization

object Authorization {

  case object PublicOnly extends Authorization
  case object All extends Authorization
  case class User(userGuid: UUID) extends Authorization

  def apply(userGuid: Option[UUID]): Authorization = {
    userGuid match {
      case None => Authorization.PublicOnly
      case Some(guid) => Authorization.User(guid)
    }
  }

}

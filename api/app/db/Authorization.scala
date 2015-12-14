package db

import java.util.UUID

sealed trait Authorization

object Authorization {

  case object PublicOnly extends Authorization
  case object All extends Authorization
  case class User(guid: UUID) extends Authorization
  case class Organization(guid: UUID) extends Authorization

  def fromUser(userGuid: Option[UUID]): Authorization = {
    userGuid match {
      case None => Authorization.PublicOnly
      case Some(guid) => Authorization.User(guid)
    }
  }

  def fromOrganization(orgGuid: Option[UUID]): Authorization = {
    orgGuid match {
      case None => Authorization.PublicOnly
      case Some(guid) => Authorization.Organization(guid)
    }
  }

}

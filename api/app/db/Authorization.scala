package db

import com.bryzek.dependency.v0.models.Visibility
import java.util.UUID

case class Clause(conditions: Seq[String]) {
  assert(!conditions.isEmpty, "Must have at least one condition")

  val sql: String = conditions match {
    case Nil => "false"
    case one :: Nil => one
    case multiple => "(" + multiple.mkString(" or ") + ")"
  }

  val and: String = s"and $sql"

}


sealed trait Authorization {

  def organizations(
    organizationGuidColumn: String,
    visibilityColumnName: Option[String] = None
  ): Clause

}


object Authorization {

  private[this] val NoRecordsClause = Clause(Seq("false"))
  private[this] val AllRecordsClause = Clause(Seq("true"))

  private[this] def publicVisibilityClause(column: String) = {
    s"$column = '${Visibility.Public}'"
  }

  case object PublicOnly extends Authorization {

    override def organizations(
      organizationGuidColumn: String,
      visibilityColumnName: Option[String] = None
    ): Clause = {
      visibilityColumnName match {
        case None => NoRecordsClause
        case Some(col) => Clause(Seq(publicVisibilityClause(col)))
      }
    }

  }

  case object All extends Authorization {

    override def organizations(
      organizationGuidColumn: String,
      visibilityColumnName: Option[String] = None
    ): Clause = AllRecordsClause

  }

  case class User(guid: UUID) extends Authorization {

    override def organizations(
      organizationGuidColumn: String,
      visibilityColumnName: Option[String] = None
    ): Clause = {
      val userClause = s"$organizationGuidColumn in (select organization_guid from memberships where deleted_at is null and user_guid = '$guid')"
      visibilityColumnName match {
        case None => Clause(Seq(userClause))
        case Some(col) => Clause(Seq(userClause, publicVisibilityClause(col)))
      }
    }

  }

  case class Organization(guid: UUID) extends Authorization {

    override def organizations(
      organizationGuidColumn: String,
      visibilityColumnName: Option[String] = None
    ): Clause = {
      val orgClause = s"$organizationGuidColumn = '$guid'"
      visibilityColumnName match {
        case None => Clause(Seq(orgClause))
        case Some(col) => Clause(Seq(orgClause, publicVisibilityClause(col)))
      }
    }

  }

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

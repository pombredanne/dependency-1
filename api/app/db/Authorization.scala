package db

import com.bryzek.dependency.v0.models.Visibility

trait Clause {

  def sql: String

  def and: String = s"and $sql"

}

object Clause {

  case object True extends Clause {
    override val sql: String = "true"
  }

  case object False extends Clause {
    override val sql: String = "false"
  }

  case class Single(condition: String) extends Clause {
    assert(!condition.trim.isEmpty, "condition cannot be empty")
    override val sql: String = condition
  }

  case class Or(conditions: Seq[String]) extends Clause {
    assert(!conditions.isEmpty, "Must have at least one condition")

    override val sql: String = conditions match {
      case Nil => "false"
      case one :: Nil => one
      case inple => "(" + inple.mkString(" or ") + ")"
    }

  }

}

sealed trait Authorization {

  def organizations(
    organizationIdColumn: String,
    visibilityColumnName: Option[String] = None
  ): Clause

}


object Authorization {

  private[this] val NoRecordsClause = Clause.False
  private[this] val AllRecordsClause = Clause.True

  private[this] def publicVisibilityClause(column: String) = {
    s"$column = '${Visibility.Public}'"
  }

  case object PublicOnly extends Authorization {

    override def organizations(
      organizationIdColumn: String,
      visibilityColumnName: Option[String] = None
    ): Clause = {
      visibilityColumnName match {
        case None => NoRecordsClause
        case Some(col) => Clause.Single(publicVisibilityClause(col))
      }
    }

  }

  case object All extends Authorization {

    override def organizations(
      organizationIdColumn: String,
      visibilityColumnName: Option[String] = None
    ): Clause = AllRecordsClause

  }

  case class User(id: String) extends Authorization {

    override def organizations(
      organizationIdColumn: String,
      visibilityColumnName: Option[String] = None
    ): Clause = {
      // TODO: Bind
      val userClause = s"$organizationIdColumn in (select organization_id from memberships where deleted_at is null and user_id = '$id')"
      visibilityColumnName match {
        case None => Clause.Single(userClause)
        case Some(col) => Clause.Or(Seq(userClause, publicVisibilityClause(col)))
      }
    }

  }

  case class Organization(id: String) extends Authorization {

    override def organizations(
      organizationIdColumn: String,
      visibilityColumnName: Option[String] = None
    ): Clause = {
      val orgClause = s"$organizationIdColumn = '$id'"
      visibilityColumnName match {
        case None => Clause.Single(orgClause)
        case Some(col) => Clause.Or(Seq(orgClause, publicVisibilityClause(col)))
      }
    }

  }

  def fromUser(userId: Option[String]): Authorization = {
    userId match {
      case None => Authorization.PublicOnly
      case Some(id) => Authorization.User(id)
    }
  }

  def fromOrganization(orgId: Option[String]): Authorization = {
    orgId match {
      case None => Authorization.PublicOnly
      case Some(id) => Authorization.Organization(id)
    }
  }

}

package com.bryzek.dependency.lib

import scala.util.{Failure, Success, Try}
import scala.util.parsing.combinator.JavaTokenParsers

object QueryParser {

  def parse(input: String): Either[Seq[String], QueryParser.Clause] = {
    input.trim.toLowerCase match {
      case "" => Left(Seq("Query cannot be empty"))
      case value => {
        val parser = new QueryParser()
        parser.parseAll(parser.clause, value) match {
          case parser.Success(result, _) => validate(result) match {
            case Nil => Right(result)
            case errors => Left(errors)
          }
          case parser.Error(msg, _) => Left(Seq(msg))
          case parser.Failure(msg, _) => Left(parseFailure(input, msg))
        }
      }
    }
  }

  /**
    * Takes as input a query and the generated error message. Turns
    * that into a human understandable error.
    */
  private def parseFailure(originalInput: String, error: String): Seq[String] = {
    val input = originalInput.trim.toLowerCase
    val numberOpened = input.count(_ == '(')
    val numberClosed = input.count(_ == ')')

    if (numberOpened != numberClosed) {
      Seq(s"Invalid query: Unbalanced parens opened[$numberOpened] closed[$numberClosed]")
    } else {
      if (input.endsWith("and")) {
        Seq("Invalid query: Cannot end with 'and'")

      } else if (input.endsWith("or")) {
        Seq("Invalid query: Cannot end with 'or'")

      } else if (!hasOperand(input)) {
        Seq("Invalid query: must include at least one operand")

      } else if (contains(input, "and and")) {
        Seq("Invalid query: Duplicate 'and'")

      } else if (contains(input, "or or")) {
        Seq("Invalid query: Duplicate 'or'")

      } else if (contains(input, "and or")) {
        Seq("Invalid query: 'and' cannot be followed by 'or'")

      } else if (contains(input, "or and")) {
        Seq("Invalid query: 'or' cannot be followed by 'and'")

      } else {

        play.api.Logger.warn(s"Invalid query[$input] error[$error]")
        Seq(s"Invalid query")
      }
    }
  }

  private def hasOperand(input: String): Boolean = {
    QueryParser.ComparisonOperand.all.find { op =>
      input.indexOf(op.label) >= 0
    } match {
      case None => false
      case Some(_) => true
    }
  }

  private def contains(input: String, clause: String): Boolean = {
    assert(clause.trim.toLowerCase == clause, s"clause[$clause] must be in lower case, trimmed")
    input.replaceAll("\\s+", " ").indexOf(clause) >= 0
  }

  private def validateField(field: QueryParser.Field): Seq[String] = {
    val typeErrors: Seq[String] = field.objectType match {
      case ObjectType.Items => Nil
      case ObjectType.Unknown => Seq("Queries criteria must match <object type>.<field> where object type is one of: " + QueryParser.ObjectType.all.map(_.label).mkString(", "))
      case ObjectType.UNDEFINED(value) => Seq(s"Unrecognized prefix[$value]. Expected one of: " + QueryParser.ObjectType.all.map(_.label).mkString(", "))
    }

    typeErrors match {
      case Nil => {
        field.objectType.fields.contains(field.name) match {
          case true => Nil
          case false =>Seq(s"${field.objectType.label} field[${field.name}] not recognized. Must be one of: " + field.objectType.fields.mkString(", "))
        }
      }
      case errors => errors
    }
  }

  private def validateComparisonOperand(op: ComparisonOperand): Seq[String] = {
    op match {
      case QueryParser.ComparisonOperand.UNDEFINED(value) => {
        Seq(s"Operand[$value] not recognized. Must be one of: " + QueryParser.ComparisonOperand.all.map(_.label).mkString(", "))
      }
      case _ => Nil
    }
  }

  private def validate(clause: QueryParser.Clause, errors: Seq[String] = Nil): Seq[String] = {
    clause match {
      case QueryParser.Clause.And(left, right) => {
        errors ++ validate(left) ++ validate(right)
      }
      case QueryParser.Clause.Or(left, right) => {
        errors ++ validate(left) ++ validate(right)
      }
      case QueryParser.Clause.StringClause(field, operand, value) => {
        errors ++ validateField(field) ++ validateComparisonOperand(operand)
      }
      case QueryParser.Clause.InvalidClause(field, operand, value, error) => {
        errors ++ Seq(error)
      }

    }
  }

  sealed trait ComparisonOperand {
    def label: String
  }

  object ComparisonOperand {
    case object NotEquals extends ComparisonOperand { override def label = "!=" }
    case object Equals extends ComparisonOperand { override def label = "=" }

    case object LessThan extends ComparisonOperand { override def label = "<" }
    case object LessThanOrEquals extends ComparisonOperand { override def label = "<=" }

    case object GreaterThan extends ComparisonOperand { override def label = ">" }
    case object GreaterThanOrEquals extends ComparisonOperand { override def label = ">=" }

    case class UNDEFINED(value: String) extends ComparisonOperand { override def label = value }

    val all = Seq(NotEquals, Equals, LessThan, LessThanOrEquals, GreaterThan, GreaterThanOrEquals)

    def apply(value: String): ComparisonOperand = fromString(value).getOrElse {
      UNDEFINED(value)
    }

    def fromString(value: String): Option[ComparisonOperand] = all.find(_.label == value)
  }

  sealed trait ObjectType {
    def label: String
    def fields: Seq[String]
  }

  object ObjectType {
    case object Items extends ObjectType {
      override def label = "items"
      override def fields = Seq("height", "length", "weight", "width")
    }

    case object Unknown extends ObjectType {
      override def label = "__unknown__"
      override def fields = Nil
    }

    case class UNDEFINED(value: String) extends ObjectType {
      override def label = value
      override def fields = Nil
    }

    val all = Seq(Items)

    def apply(value: String): ObjectType = fromString(value).getOrElse {
      UNDEFINED(value)
    }

    def fromString(value: String): Option[ObjectType] = all.find(_.label == value)

  }

  // e.g. Field("items", name)
  case class Field(objectType: ObjectType, name: String) {
    val label = {
      objectType match {
        case ObjectType.Items => s"${objectType.label}.$name"
        case ObjectType.Unknown => name
        case ObjectType.UNDEFINED(value) => s"${value}.$name"
      }
    }
  }

  sealed trait Clause {
    def label: String
  }

  object Clause {

    case class And(left: Clause, right: Clause) extends Clause {
      override def label() = {
        s"${left.label} and ${right.label}"
      }
    }

    case class Or(left: Clause, right: Clause) extends Clause {
      override def label() = {
        s"(${left.label}) or (${right.label})"
      }
    }

    case class StringClause(field: Field, operand: ComparisonOperand, value: String) extends Clause {
      override def label() = {
        s"${field.label} ${operand.label} $value"
      }
    }

    case class InvalidClause(field: Field, operand: ComparisonOperand, value: String, error: String) extends Clause {
      override def label() = {
        StringClause(field, operand, value).label
      }
    }

  }
}

class QueryParser extends JavaTokenParsers {
  import QueryParser.{Clause, ObjectType, ComparisonOperand, Field}

  def generalStringIdent = """(?!and|or)[a-z0-9\.]+""".r // Excludes "and", "or"

  def clause: Parser[Clause] = (predicate|parens) * (
    "and" ^^^ { (a: Clause, b: Clause) => Clause.And(a,b) } |
    "or" ^^^ { (a: Clause, b: Clause) => Clause.Or(a,b) }
  )

  def parens: Parser[Clause] = "(" ~> clause  <~ ")"

  def fieldIdent: Parser[Field] = ident ~! opt("." ~! ident) ^^ {
    case l ~ list => {
      list match {
        case None => {
          Field(ObjectType.Unknown, l)
        }
        case Some(foo) => {
          Field(ObjectType(l), foo._2)
        }
      }
    }
  }

  def comparisonOperand: Parser[ComparisonOperand] = """[\<\=\!\>\s]+""".r ^^ { op =>
    ComparisonOperand(op.toString.replaceAll(" ", "").trim)
  }

  def predicate = (
    fieldIdent ~ comparisonOperand ~ rep(generalStringIdent) ^^ {
      case f ~ op ~ values => {
        values.map(stripQuotes(_)) match {
          case Nil => {
            Clause.InvalidClause(f, op, "", "Missing number and units: items.weight < <number> <units>")
          }
          case value :: Nil => {
            Clause.StringClause(f, op, value)
          }
          case multiple => {
            Clause.InvalidClause(f, op, multiple.mkString(" "), "Too many elements")
          }
        }
      }
    }
  )

  @scala.annotation.tailrec
  private def stripQuotes(s: String): String = {
    if (s.startsWith("\"") && s.endsWith("\"")) {
      stripQuotes(s.substring(1, s.length - 1))
    } else {
      s
    }
  }

}
        

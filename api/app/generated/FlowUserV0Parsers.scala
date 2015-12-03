import anorm._

package io.flow.user.v0.anorm.parsers {

  import io.flow.user.v0.anorm.conversions.Json._

  object System {

    case class Mappings(value: String)

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        value = s"${prefix}${sep}value"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[io.flow.user.v0.models.System] = {
      SqlParser.str(mappings.value) map {
        case value => io.flow.user.v0.models.System(value)
      }
    }

  }
  object Name {

    case class Mappings(
      first: String = "first",
      last: String = "last"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        first = s"${prefix}${sep}first",
        last = s"${prefix}${sep}last"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[io.flow.user.v0.models.Name] = {
      SqlParser.str(mappings.first).? ~
      SqlParser.str(mappings.last).? map {
        case first ~ last => {
          io.flow.user.v0.models.Name(
            first = first,
            last = last
          )
        }
      }
    }

  }

  object NameForm {

    case class Mappings(
      first: String = "first",
      last: String = "last"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        first = s"${prefix}${sep}first",
        last = s"${prefix}${sep}last"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[io.flow.user.v0.models.NameForm] = {
      SqlParser.str(mappings.first).? ~
      SqlParser.str(mappings.last).? map {
        case first ~ last => {
          io.flow.user.v0.models.NameForm(
            first = first,
            last = last
          )
        }
      }
    }

  }

  object User {

    case class Mappings(
      guid: String = "guid",
      email: String = "email",
      name: io.flow.user.v0.anorm.parsers.Name.Mappings,
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        email = s"${prefix}${sep}email",
        name = io.flow.user.v0.anorm.parsers.Name.Mappings.prefix(Seq(prefix, "name").filter(!_.isEmpty).mkString("_"), "_"),
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[io.flow.user.v0.models.User] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      SqlParser.str(mappings.email).? ~
      io.flow.user.v0.anorm.parsers.Name.parser(mappings.name) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ email ~ name ~ audit => {
          io.flow.user.v0.models.User(
            guid = guid,
            email = email,
            name = name,
            audit = audit
          )
        }
      }
    }

  }

  object UserForm {

    case class Mappings(
      email: String = "email",
      name: io.flow.user.v0.anorm.parsers.NameForm.Mappings,
      avatarUrl: String = "avatarUrl"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        email = s"${prefix}${sep}email",
        name = io.flow.user.v0.anorm.parsers.NameForm.Mappings.prefix(Seq(prefix, "name").filter(!_.isEmpty).mkString("_"), "_"),
        avatarUrl = s"${prefix}${sep}avatar_url"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[io.flow.user.v0.models.UserForm] = {
      SqlParser.str(mappings.email).? ~
      io.flow.user.v0.anorm.parsers.NameForm.parser(mappings.name).? ~
      SqlParser.str(mappings.avatarUrl).? map {
        case email ~ name ~ avatarUrl => {
          io.flow.user.v0.models.UserForm(
            email = email,
            name = name,
            avatarUrl = avatarUrl
          )
        }
      }
    }

  }

}
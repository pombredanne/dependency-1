import anorm._

package com.bryzek.dependency.v0.anorm.parsers {

  object ProgrammingLanguage {

    case class Mappings(value: String)

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        value = s"${prefix}${sep}value"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ProgrammingLanguage] = {
      SqlParser.str(mappings.value) map {
        case value => com.bryzek.dependency.v0.models.ProgrammingLanguage(value)
      }
    }

  }
  object Scms {

    case class Mappings(value: String)

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        value = s"${prefix}${sep}value"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Scms] = {
      SqlParser.str(mappings.value) map {
        case value => com.bryzek.dependency.v0.models.Scms(value)
      }
    }

  }

  object AuthenticationForm {

    case class Mappings(
      email: String = "email"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        email = s"${prefix}${sep}email"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.AuthenticationForm] = {
      SqlParser.str(mappings.email) map {
        case email => {
          com.bryzek.dependency.v0.models.AuthenticationForm(
            email = email
          )
        }
      }
    }

  }

  object Language {

    case class Mappings(
      guid: String = "guid",
      name: String = "name",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        name = s"${prefix}${sep}name",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Language] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.ProgrammingLanguage.parser(com.bryzek.dependency.v0.anorm.parsers.ProgrammingLanguage.Mappings(mappings.name)) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ name ~ audit => {
          com.bryzek.dependency.v0.models.Language(
            guid = guid,
            name = name,
            audit = audit
          )
        }
      }
    }

  }

  object LanguageForm {

    case class Mappings(
      name: String = "name",
      version: String = "version"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        name = s"${prefix}${sep}name",
        version = s"${prefix}${sep}version"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LanguageForm] = {
      SqlParser.str(mappings.name) ~
      SqlParser.str(mappings.version).? map {
        case name ~ version => {
          com.bryzek.dependency.v0.models.LanguageForm(
            name = name,
            version = version
          )
        }
      }
    }

  }

  object LanguageVersion {

    case class Mappings(
      guid: String = "guid",
      version: String = "version",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        version = s"${prefix}${sep}version",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LanguageVersion] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      SqlParser.str(mappings.version) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ version ~ audit => {
          com.bryzek.dependency.v0.models.LanguageVersion(
            guid = guid,
            version = version,
            audit = audit
          )
        }
      }
    }

  }

  object Library {

    case class Mappings(
      guid: String = "guid",
      resolvers: String = "resolvers",
      groupId: String = "groupId",
      artifactId: String = "artifactId",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        resolvers = s"${prefix}${sep}resolvers",
        groupId = s"${prefix}${sep}group_id",
        artifactId = s"${prefix}${sep}artifact_id",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Library] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      SqlParser.list[String](mappings.resolvers) ~
      SqlParser.str(mappings.groupId) ~
      SqlParser.str(mappings.artifactId) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ resolvers ~ groupId ~ artifactId ~ audit => {
          com.bryzek.dependency.v0.models.Library(
            guid = guid,
            resolvers = resolvers,
            groupId = groupId,
            artifactId = artifactId,
            audit = audit
          )
        }
      }
    }

  }

  object LibraryForm {

    case class Mappings(
      groupId: String = "groupId",
      resolvers: String = "resolvers",
      artifactId: String = "artifactId",
      version: String = "version"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        groupId = s"${prefix}${sep}group_id",
        resolvers = s"${prefix}${sep}resolvers",
        artifactId = s"${prefix}${sep}artifact_id",
        version = s"${prefix}${sep}version"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LibraryForm] = {
      SqlParser.str(mappings.groupId) ~
      SqlParser.list[String](mappings.resolvers) ~
      SqlParser.str(mappings.artifactId) ~
      SqlParser.str(mappings.version).? map {
        case groupId ~ resolvers ~ artifactId ~ version => {
          com.bryzek.dependency.v0.models.LibraryForm(
            groupId = groupId,
            resolvers = resolvers,
            artifactId = artifactId,
            version = version
          )
        }
      }
    }

  }

  object LibraryVersion {

    case class Mappings(
      guid: String = "guid",
      version: String = "version",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        version = s"${prefix}${sep}version",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LibraryVersion] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      SqlParser.str(mappings.version) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ version ~ audit => {
          com.bryzek.dependency.v0.models.LibraryVersion(
            guid = guid,
            version = version,
            audit = audit
          )
        }
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

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Name] = {
      SqlParser.str(mappings.first).? ~
      SqlParser.str(mappings.last).? map {
        case first ~ last => {
          com.bryzek.dependency.v0.models.Name(
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

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.NameForm] = {
      SqlParser.str(mappings.first).? ~
      SqlParser.str(mappings.last).? map {
        case first ~ last => {
          com.bryzek.dependency.v0.models.NameForm(
            first = first,
            last = last
          )
        }
      }
    }

  }

  object Project {

    case class Mappings(
      guid: String = "guid",
      scms: String = "scms",
      name: String = "name",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        scms = s"${prefix}${sep}scms",
        name = s"${prefix}${sep}name",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Project] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.Scms.parser(com.bryzek.dependency.v0.anorm.parsers.Scms.Mappings(mappings.scms)) ~
      SqlParser.str(mappings.name) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ scms ~ name ~ audit => {
          com.bryzek.dependency.v0.models.Project(
            guid = guid,
            scms = scms,
            name = name,
            audit = audit
          )
        }
      }
    }

  }

  object ProjectForm {

    case class Mappings(
      name: String = "name",
      scms: String = "scms",
      uri: String = "uri"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        name = s"${prefix}${sep}name",
        scms = s"${prefix}${sep}scms",
        uri = s"${prefix}${sep}uri"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ProjectForm] = {
      SqlParser.str(mappings.name) ~
      com.bryzek.dependency.v0.anorm.parsers.Scms.parser(com.bryzek.dependency.v0.anorm.parsers.Scms.Mappings(mappings.scms)) ~
      SqlParser.str(mappings.uri) map {
        case name ~ scms ~ uri => {
          com.bryzek.dependency.v0.models.ProjectForm(
            name = name,
            scms = scms,
            uri = uri
          )
        }
      }
    }

  }

  object User {

    case class Mappings(
      guid: String = "guid",
      email: String = "email",
      name: com.bryzek.dependency.v0.anorm.parsers.Name.Mappings,
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        email = s"${prefix}${sep}email",
        name = com.bryzek.dependency.v0.anorm.parsers.Name.Mappings.prefix(Seq(prefix, "name").filter(!_.isEmpty).mkString("_"), "_"),
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.User] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      SqlParser.str(mappings.email) ~
      com.bryzek.dependency.v0.anorm.parsers.Name.parser(mappings.name) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ email ~ name ~ audit => {
          com.bryzek.dependency.v0.models.User(
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
      name: com.bryzek.dependency.v0.anorm.parsers.NameForm.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        email = s"${prefix}${sep}email",
        name = com.bryzek.dependency.v0.anorm.parsers.NameForm.Mappings.prefix(Seq(prefix, "name").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.UserForm] = {
      SqlParser.str(mappings.email) ~
      com.bryzek.dependency.v0.anorm.parsers.NameForm.parser(mappings.name).? map {
        case email ~ name => {
          com.bryzek.dependency.v0.models.UserForm(
            email = email,
            name = name
          )
        }
      }
    }

  }

}
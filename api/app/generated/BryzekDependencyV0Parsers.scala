import anorm._

package com.bryzek.dependency.v0.anorm {

  package parsers {

    object ProgrammingLanguage {

      def newParser(name: String) = parser(name)

      def parserByTable(table: String) = parser(s"$table.programming_language")

      def parser(name: String): RowParser[com.bryzek.dependency.v0.models.ProgrammingLanguage] = {
        SqlParser.str(name) map {
          case value => com.bryzek.dependency.v0.models.ProgrammingLanguage(value)
        }
      }

    }

    object Scms {

      def newParser(name: String) = parser(name)

      def parserByTable(table: String) = parser(s"$table.scms")

      def parser(name: String): RowParser[com.bryzek.dependency.v0.models.Scms] = {
        SqlParser.str(name) map {
          case value => com.bryzek.dependency.v0.models.Scms(value)
        }
      }

    }

    object Language {

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            guid = s"${prefix}_guid",
            name = s"${prefix}_name",
            audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${prefix}_audit")
          )
        }
      }

      def parserByTable(table: String) = parser(
        guid = s"$table.guid",
        name = s"$table.name",
        audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${table}_audit")
      )

      def parser(
        guid: String,
        name: String,
        audit: me.apidoc.lib.anorm.parsers.util.Config
      ): RowParser[com.bryzek.dependency.v0.models.Language] = {
        SqlParser.get[_root_.java.util.UUID](guid) ~
        com.bryzek.dependency.v0.anorm.parsers.ProgrammingLanguage.newParser(name) ~
        io.flow.common.v0.anorm.parsers.Audit.newParser(audit) map {
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

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            name = s"${prefix}_name",
            version = s"${prefix}_version"
          )
        }
      }

      def parserByTable(table: String) = parser(
        name = s"$table.name",
        version = s"$table.version"
      )

      def parser(
        name: String,
        version: String
      ): RowParser[com.bryzek.dependency.v0.models.LanguageForm] = {
        SqlParser.str(name) ~
        SqlParser.str(version).? map {
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

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            guid = s"${prefix}_guid",
            version = s"${prefix}_version",
            audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${prefix}_audit")
          )
        }
      }

      def parserByTable(table: String) = parser(
        guid = s"$table.guid",
        version = s"$table.version",
        audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${table}_audit")
      )

      def parser(
        guid: String,
        version: String,
        audit: me.apidoc.lib.anorm.parsers.util.Config
      ): RowParser[com.bryzek.dependency.v0.models.LanguageVersion] = {
        SqlParser.get[_root_.java.util.UUID](guid) ~
        SqlParser.str(version) ~
        io.flow.common.v0.anorm.parsers.Audit.newParser(audit) map {
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

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            guid = s"${prefix}_guid",
            resolvers = s"${prefix}_resolvers",
            groupId = s"${prefix}_groupId",
            artifactId = s"${prefix}_artifactId",
            audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${prefix}_audit")
          )
        }
      }

      def parserByTable(table: String) = parser(
        guid = s"$table.guid",
        resolvers = s"$table.resolvers",
        groupId = s"$table.groupId",
        artifactId = s"$table.artifactId",
        audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${table}_audit")
      )

      def parser(
        guid: String,
        resolvers: String,
        groupId: String,
        artifactId: String,
        audit: me.apidoc.lib.anorm.parsers.util.Config
      ): RowParser[com.bryzek.dependency.v0.models.Library] = {
        SqlParser.get[_root_.java.util.UUID](guid) ~
        SqlParser.get[String].list(resolvers) ~
        SqlParser.str(groupId) ~
        SqlParser.str(artifactId) ~
        io.flow.common.v0.anorm.parsers.Audit.newParser(audit) map {
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

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            groupId = s"${prefix}_groupId",
            resolvers = s"${prefix}_resolvers",
            artifactId = s"${prefix}_artifactId",
            version = s"${prefix}_version"
          )
        }
      }

      def parserByTable(table: String) = parser(
        groupId = s"$table.groupId",
        resolvers = s"$table.resolvers",
        artifactId = s"$table.artifactId",
        version = s"$table.version"
      )

      def parser(
        groupId: String,
        resolvers: String,
        artifactId: String,
        version: String
      ): RowParser[com.bryzek.dependency.v0.models.LibraryForm] = {
        SqlParser.str(groupId) ~
        SqlParser.get[String].list(resolvers) ~
        SqlParser.str(artifactId) ~
        SqlParser.str(version).? map {
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

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            guid = s"${prefix}_guid",
            version = s"${prefix}_version",
            audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${prefix}_audit")
          )
        }
      }

      def parserByTable(table: String) = parser(
        guid = s"$table.guid",
        version = s"$table.version",
        audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${table}_audit")
      )

      def parser(
        guid: String,
        version: String,
        audit: me.apidoc.lib.anorm.parsers.util.Config
      ): RowParser[com.bryzek.dependency.v0.models.LibraryVersion] = {
        SqlParser.get[_root_.java.util.UUID](guid) ~
        SqlParser.str(version) ~
        io.flow.common.v0.anorm.parsers.Audit.newParser(audit) map {
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

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            first = s"${prefix}_first",
            last = s"${prefix}_last"
          )
        }
      }

      def parserByTable(table: String) = parser(
        first = s"$table.first",
        last = s"$table.last"
      )

      def parser(
        first: String,
        last: String
      ): RowParser[com.bryzek.dependency.v0.models.Name] = {
        SqlParser.str(first).? ~
        SqlParser.str(last).? map {
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

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            first = s"${prefix}_first",
            last = s"${prefix}_last"
          )
        }
      }

      def parserByTable(table: String) = parser(
        first = s"$table.first",
        last = s"$table.last"
      )

      def parser(
        first: String,
        last: String
      ): RowParser[com.bryzek.dependency.v0.models.NameForm] = {
        SqlParser.str(first).? ~
        SqlParser.str(last).? map {
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

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            guid = s"${prefix}_guid",
            scms = s"${prefix}_scms",
            name = s"${prefix}_name",
            audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${prefix}_audit")
          )
        }
      }

      def parserByTable(table: String) = parser(
        guid = s"$table.guid",
        scms = s"$table.scms",
        name = s"$table.name",
        audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${table}_audit")
      )

      def parser(
        guid: String,
        scms: String,
        name: String,
        audit: me.apidoc.lib.anorm.parsers.util.Config
      ): RowParser[com.bryzek.dependency.v0.models.Project] = {
        SqlParser.get[_root_.java.util.UUID](guid) ~
        com.bryzek.dependency.v0.anorm.parsers.Scms.newParser(scms) ~
        SqlParser.str(name) ~
        io.flow.common.v0.anorm.parsers.Audit.newParser(audit) map {
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

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            name = s"${prefix}_name",
            scms = s"${prefix}_scms",
            uri = s"${prefix}_uri"
          )
        }
      }

      def parserByTable(table: String) = parser(
        name = s"$table.name",
        scms = s"$table.scms",
        uri = s"$table.uri"
      )

      def parser(
        name: String,
        scms: String,
        uri: String
      ): RowParser[com.bryzek.dependency.v0.models.ProjectForm] = {
        SqlParser.str(name) ~
        com.bryzek.dependency.v0.anorm.parsers.Scms.newParser(scms) ~
        SqlParser.str(uri) map {
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

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            guid = s"${prefix}_guid",
            email = s"${prefix}_email",
            name = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${prefix}_name"),
            audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${prefix}_audit")
          )
        }
      }

      def parserByTable(table: String) = parser(
        guid = s"$table.guid",
        email = s"$table.email",
        name = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${table}_name"),
        audit = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${table}_audit")
      )

      def parser(
        guid: String,
        email: String,
        name: me.apidoc.lib.anorm.parsers.util.Config,
        audit: me.apidoc.lib.anorm.parsers.util.Config
      ): RowParser[com.bryzek.dependency.v0.models.User] = {
        SqlParser.get[_root_.java.util.UUID](guid) ~
        SqlParser.str(email) ~
        com.bryzek.dependency.v0.anorm.parsers.Name.newParser(name).? ~
        io.flow.common.v0.anorm.parsers.Audit.newParser(audit) map {
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

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            guid = s"${prefix}_guid",
            email = s"${prefix}_email",
            name = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${prefix}_name")
          )
        }
      }

      def parserByTable(table: String) = parser(
        guid = s"$table.guid",
        email = s"$table.email",
        name = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${table}_name")
      )

      def parser(
        guid: String,
        email: String,
        name: me.apidoc.lib.anorm.parsers.util.Config
      ): RowParser[com.bryzek.dependency.v0.models.UserForm] = {
        SqlParser.get[_root_.java.util.UUID](guid) ~
        SqlParser.str(email) ~
        com.bryzek.dependency.v0.anorm.parsers.NameForm.newParser(name).? map {
          case guid ~ email ~ name => {
            com.bryzek.dependency.v0.models.UserForm(
              guid = guid,
              email = email,
              name = name
            )
          }
        }
      }

    }

  }

}
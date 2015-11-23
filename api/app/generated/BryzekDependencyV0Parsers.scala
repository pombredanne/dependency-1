import anorm._

package com.bryzek.dependency.v0.anorm.parsers {

  import com.bryzek.dependency.v0.anorm.conversions.Json._

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
      SqlParser.str(mappings.version) map {
        case name ~ version => {
          com.bryzek.dependency.v0.models.LanguageForm(
            name = name,
            version = version
          )
        }
      }
    }

  }

  object LanguageRecommendation {

    case class Mappings(
      from: com.bryzek.dependency.v0.anorm.parsers.LanguageVersion.Mappings,
      to: com.bryzek.dependency.v0.anorm.parsers.LanguageVersion.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        from = com.bryzek.dependency.v0.anorm.parsers.LanguageVersion.Mappings.prefix(Seq(prefix, "from").filter(!_.isEmpty).mkString("_"), "_"),
        to = com.bryzek.dependency.v0.anorm.parsers.LanguageVersion.Mappings.prefix(Seq(prefix, "to").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LanguageRecommendation] = {
      com.bryzek.dependency.v0.anorm.parsers.LanguageVersion.parser(mappings.from) ~
      com.bryzek.dependency.v0.anorm.parsers.LanguageVersion.parser(mappings.to) map {
        case from ~ to => {
          com.bryzek.dependency.v0.models.LanguageRecommendation(
            from = from,
            to = to
          )
        }
      }
    }

  }

  object LanguageVersion {

    case class Mappings(
      guid: String = "guid",
      language: com.bryzek.dependency.v0.anorm.parsers.Language.Mappings,
      version: String = "version",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        language = com.bryzek.dependency.v0.anorm.parsers.Language.Mappings.prefix(Seq(prefix, "language").filter(!_.isEmpty).mkString("_"), "_"),
        version = s"${prefix}${sep}version",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LanguageVersion] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.Language.parser(mappings.language) ~
      SqlParser.str(mappings.version) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ language ~ version ~ audit => {
          com.bryzek.dependency.v0.models.LanguageVersion(
            guid = guid,
            language = language,
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
      SqlParser.get[Seq[String]](mappings.resolvers) ~
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
      version: com.bryzek.dependency.v0.anorm.parsers.VersionForm.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        groupId = s"${prefix}${sep}group_id",
        resolvers = s"${prefix}${sep}resolvers",
        artifactId = s"${prefix}${sep}artifact_id",
        version = com.bryzek.dependency.v0.anorm.parsers.VersionForm.Mappings.prefix(Seq(prefix, "version").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LibraryForm] = {
      SqlParser.str(mappings.groupId) ~
      SqlParser.get[Seq[String]](mappings.resolvers) ~
      SqlParser.str(mappings.artifactId) ~
      com.bryzek.dependency.v0.anorm.parsers.VersionForm.parser(mappings.version).? map {
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

  object LibraryRecommendation {

    case class Mappings(
      from: com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.Mappings,
      to: com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        from = com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.Mappings.prefix(Seq(prefix, "from").filter(!_.isEmpty).mkString("_"), "_"),
        to = com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.Mappings.prefix(Seq(prefix, "to").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LibraryRecommendation] = {
      com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.parser(mappings.from) ~
      com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.parser(mappings.to) map {
        case from ~ to => {
          com.bryzek.dependency.v0.models.LibraryRecommendation(
            from = from,
            to = to
          )
        }
      }
    }

  }

  object LibraryVersion {

    case class Mappings(
      guid: String = "guid",
      library: com.bryzek.dependency.v0.anorm.parsers.Library.Mappings,
      version: String = "version",
      crossBuildVersion: String = "crossBuildVersion",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        library = com.bryzek.dependency.v0.anorm.parsers.Library.Mappings.prefix(Seq(prefix, "library").filter(!_.isEmpty).mkString("_"), "_"),
        version = s"${prefix}${sep}version",
        crossBuildVersion = s"${prefix}${sep}cross_build_version",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LibraryVersion] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.Library.parser(mappings.library) ~
      SqlParser.str(mappings.version) ~
      SqlParser.str(mappings.crossBuildVersion).? ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ library ~ version ~ crossBuildVersion ~ audit => {
          com.bryzek.dependency.v0.models.LibraryVersion(
            guid = guid,
            library = library,
            version = version,
            crossBuildVersion = crossBuildVersion,
            audit = audit
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
      uri: String = "uri",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        scms = s"${prefix}${sep}scms",
        name = s"${prefix}${sep}name",
        uri = s"${prefix}${sep}uri",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Project] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.Scms.parser(com.bryzek.dependency.v0.anorm.parsers.Scms.Mappings(mappings.scms)) ~
      SqlParser.str(mappings.name) ~
      SqlParser.str(mappings.uri) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ scms ~ name ~ uri ~ audit => {
          com.bryzek.dependency.v0.models.Project(
            guid = guid,
            scms = scms,
            name = name,
            uri = uri,
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

  object ProjectLanguageVersion {

    case class Mappings(
      project: com.bryzek.dependency.v0.anorm.parsers.Project.Mappings,
      languageVersion: com.bryzek.dependency.v0.anorm.parsers.LanguageVersion.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        project = com.bryzek.dependency.v0.anorm.parsers.Project.Mappings.prefix(Seq(prefix, "project").filter(!_.isEmpty).mkString("_"), "_"),
        languageVersion = com.bryzek.dependency.v0.anorm.parsers.LanguageVersion.Mappings.prefix(Seq(prefix, "language_version").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ProjectLanguageVersion] = {
      com.bryzek.dependency.v0.anorm.parsers.Project.parser(mappings.project) ~
      com.bryzek.dependency.v0.anorm.parsers.LanguageVersion.parser(mappings.languageVersion) map {
        case project ~ languageVersion => {
          com.bryzek.dependency.v0.models.ProjectLanguageVersion(
            project = project,
            languageVersion = languageVersion
          )
        }
      }
    }

  }

  object ProjectLibraryVersion {

    case class Mappings(
      project: com.bryzek.dependency.v0.anorm.parsers.Project.Mappings,
      libraryVersion: com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        project = com.bryzek.dependency.v0.anorm.parsers.Project.Mappings.prefix(Seq(prefix, "project").filter(!_.isEmpty).mkString("_"), "_"),
        libraryVersion = com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.Mappings.prefix(Seq(prefix, "library_version").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ProjectLibraryVersion] = {
      com.bryzek.dependency.v0.anorm.parsers.Project.parser(mappings.project) ~
      com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.parser(mappings.libraryVersion) map {
        case project ~ libraryVersion => {
          com.bryzek.dependency.v0.models.ProjectLibraryVersion(
            project = project,
            libraryVersion = libraryVersion
          )
        }
      }
    }

  }

  object ProjectPatchForm {

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

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ProjectPatchForm] = {
      SqlParser.str(mappings.name).? ~
      com.bryzek.dependency.v0.anorm.parsers.Scms.parser(com.bryzek.dependency.v0.anorm.parsers.Scms.Mappings(mappings.scms)).? ~
      SqlParser.str(mappings.uri).? map {
        case name ~ scms ~ uri => {
          com.bryzek.dependency.v0.models.ProjectPatchForm(
            name = name,
            scms = scms,
            uri = uri
          )
        }
      }
    }

  }

  object VersionForm {

    case class Mappings(
      version: String = "version",
      crossBuildVersion: String = "crossBuildVersion"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        version = s"${prefix}${sep}version",
        crossBuildVersion = s"${prefix}${sep}cross_build_version"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.VersionForm] = {
      SqlParser.str(mappings.version) ~
      SqlParser.str(mappings.crossBuildVersion).? map {
        case version ~ crossBuildVersion => {
          com.bryzek.dependency.v0.models.VersionForm(
            version = version,
            crossBuildVersion = crossBuildVersion
          )
        }
      }
    }

  }

}
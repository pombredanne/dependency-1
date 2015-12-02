import anorm._

package com.bryzek.dependency.v0.anorm.parsers {

  import com.bryzek.dependency.v0.anorm.conversions.Json._

  object BinaryType {

    case class Mappings(value: String)

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        value = s"${prefix}${sep}value"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.BinaryType] = {
      SqlParser.str(mappings.value) map {
        case value => com.bryzek.dependency.v0.models.BinaryType(value)
      }
    }

  }
  object RecommendationType {

    case class Mappings(value: String)

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        value = s"${prefix}${sep}value"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.RecommendationType] = {
      SqlParser.str(mappings.value) map {
        case value => com.bryzek.dependency.v0.models.RecommendationType(value)
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

  object Binary {

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

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Binary] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.BinaryType.parser(com.bryzek.dependency.v0.anorm.parsers.BinaryType.Mappings(mappings.name)) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ name ~ audit => {
          com.bryzek.dependency.v0.models.Binary(
            guid = guid,
            name = name,
            audit = audit
          )
        }
      }
    }

  }

  object BinaryForm {

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

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.BinaryForm] = {
      SqlParser.str(mappings.name) ~
      SqlParser.str(mappings.version) map {
        case name ~ version => {
          com.bryzek.dependency.v0.models.BinaryForm(
            name = name,
            version = version
          )
        }
      }
    }

  }

  object BinaryRecommendation {

    case class Mappings(
      from: com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.Mappings,
      to: com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.Mappings,
      latest: com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        from = com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.Mappings.prefix(Seq(prefix, "from").filter(!_.isEmpty).mkString("_"), "_"),
        to = com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.Mappings.prefix(Seq(prefix, "to").filter(!_.isEmpty).mkString("_"), "_"),
        latest = com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.Mappings.prefix(Seq(prefix, "latest").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.BinaryRecommendation] = {
      com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.parser(mappings.from) ~
      com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.parser(mappings.to) ~
      com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.parser(mappings.latest) map {
        case from ~ to ~ latest => {
          com.bryzek.dependency.v0.models.BinaryRecommendation(
            from = from,
            to = to,
            latest = latest
          )
        }
      }
    }

  }

  object BinaryVersion {

    case class Mappings(
      guid: String = "guid",
      binary: com.bryzek.dependency.v0.anorm.parsers.Binary.Mappings,
      version: String = "version",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        binary = com.bryzek.dependency.v0.anorm.parsers.Binary.Mappings.prefix(Seq(prefix, "binary").filter(!_.isEmpty).mkString("_"), "_"),
        version = s"${prefix}${sep}version",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.BinaryVersion] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.Binary.parser(mappings.binary) ~
      SqlParser.str(mappings.version) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ binary ~ version ~ audit => {
          com.bryzek.dependency.v0.models.BinaryVersion(
            guid = guid,
            binary = binary,
            version = version,
            audit = audit
          )
        }
      }
    }

  }

  object GithubAuthenticationForm {

    case class Mappings(
      code: String = "code"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        code = s"${prefix}${sep}code"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.GithubAuthenticationForm] = {
      SqlParser.str(mappings.code) map {
        case code => {
          com.bryzek.dependency.v0.models.GithubAuthenticationForm(
            code = code
          )
        }
      }
    }

  }

  object GithubUser {

    case class Mappings(
      guid: String = "guid",
      user: io.flow.common.v0.anorm.parsers.Reference.Mappings,
      id: String = "id",
      login: String = "login",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        user = io.flow.common.v0.anorm.parsers.Reference.Mappings.prefix(Seq(prefix, "user").filter(!_.isEmpty).mkString("_"), "_"),
        id = s"${prefix}${sep}id",
        login = s"${prefix}${sep}login",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.GithubUser] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      io.flow.common.v0.anorm.parsers.Reference.parser(mappings.user) ~
      SqlParser.long(mappings.id) ~
      SqlParser.str(mappings.login) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ user ~ id ~ login ~ audit => {
          com.bryzek.dependency.v0.models.GithubUser(
            guid = guid,
            user = user,
            id = id,
            login = login,
            audit = audit
          )
        }
      }
    }

  }

  object GithubUserForm {

    case class Mappings(
      userGuid: String = "userGuid",
      id: String = "id",
      login: String = "login"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        userGuid = s"${prefix}${sep}user_guid",
        id = s"${prefix}${sep}id",
        login = s"${prefix}${sep}login"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.GithubUserForm] = {
      SqlParser.get[_root_.java.util.UUID](mappings.userGuid) ~
      SqlParser.long(mappings.id) ~
      SqlParser.str(mappings.login) map {
        case userGuid ~ id ~ login => {
          com.bryzek.dependency.v0.models.GithubUserForm(
            userGuid = userGuid,
            id = id,
            login = login
          )
        }
      }
    }

  }

  object Library {

    case class Mappings(
      guid: String = "guid",
      groupId: String = "groupId",
      artifactId: String = "artifactId",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        groupId = s"${prefix}${sep}group_id",
        artifactId = s"${prefix}${sep}artifact_id",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Library] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      SqlParser.str(mappings.groupId) ~
      SqlParser.str(mappings.artifactId) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ groupId ~ artifactId ~ audit => {
          com.bryzek.dependency.v0.models.Library(
            guid = guid,
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
      artifactId: String = "artifactId",
      version: com.bryzek.dependency.v0.anorm.parsers.VersionForm.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        groupId = s"${prefix}${sep}group_id",
        artifactId = s"${prefix}${sep}artifact_id",
        version = com.bryzek.dependency.v0.anorm.parsers.VersionForm.Mappings.prefix(Seq(prefix, "version").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LibraryForm] = {
      SqlParser.str(mappings.groupId) ~
      SqlParser.str(mappings.artifactId) ~
      com.bryzek.dependency.v0.anorm.parsers.VersionForm.parser(mappings.version).? map {
        case groupId ~ artifactId ~ version => {
          com.bryzek.dependency.v0.models.LibraryForm(
            groupId = groupId,
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
      to: com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.Mappings,
      latest: com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        from = com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.Mappings.prefix(Seq(prefix, "from").filter(!_.isEmpty).mkString("_"), "_"),
        to = com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.Mappings.prefix(Seq(prefix, "to").filter(!_.isEmpty).mkString("_"), "_"),
        latest = com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.Mappings.prefix(Seq(prefix, "latest").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LibraryRecommendation] = {
      com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.parser(mappings.from) ~
      com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.parser(mappings.to) ~
      com.bryzek.dependency.v0.anorm.parsers.LibraryVersion.parser(mappings.latest) map {
        case from ~ to ~ latest => {
          com.bryzek.dependency.v0.models.LibraryRecommendation(
            from = from,
            to = to,
            latest = latest
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

  object ProjectBinaryVersion {

    case class Mappings(
      project: com.bryzek.dependency.v0.anorm.parsers.Project.Mappings,
      binaryVersion: com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        project = com.bryzek.dependency.v0.anorm.parsers.Project.Mappings.prefix(Seq(prefix, "project").filter(!_.isEmpty).mkString("_"), "_"),
        binaryVersion = com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.Mappings.prefix(Seq(prefix, "binary_version").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ProjectBinaryVersion] = {
      com.bryzek.dependency.v0.anorm.parsers.Project.parser(mappings.project) ~
      com.bryzek.dependency.v0.anorm.parsers.BinaryVersion.parser(mappings.binaryVersion) map {
        case project ~ binaryVersion => {
          com.bryzek.dependency.v0.models.ProjectBinaryVersion(
            project = project,
            binaryVersion = binaryVersion
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

  object Repository {

    case class Mappings(
      name: String = "name",
      uri: String = "uri"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        name = s"${prefix}${sep}name",
        uri = s"${prefix}${sep}uri"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Repository] = {
      SqlParser.str(mappings.name) ~
      SqlParser.str(mappings.uri) map {
        case name ~ uri => {
          com.bryzek.dependency.v0.models.Repository(
            name = name,
            uri = uri
          )
        }
      }
    }

  }

  object Resolver {

    case class Mappings(
      guid: String = "guid",
      user: io.flow.common.v0.anorm.parsers.Reference.Mappings,
      uri: String = "uri",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        user = io.flow.common.v0.anorm.parsers.Reference.Mappings.prefix(Seq(prefix, "user").filter(!_.isEmpty).mkString("_"), "_"),
        uri = s"${prefix}${sep}uri",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Resolver] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      io.flow.common.v0.anorm.parsers.Reference.parser(mappings.user) ~
      SqlParser.str(mappings.uri) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ user ~ uri ~ audit => {
          com.bryzek.dependency.v0.models.Resolver(
            guid = guid,
            user = user,
            uri = uri,
            audit = audit
          )
        }
      }
    }

  }

  object ResolverForm {

    case class Mappings(
      userGuid: String = "userGuid",
      uri: String = "uri"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        userGuid = s"${prefix}${sep}user_guid",
        uri = s"${prefix}${sep}uri"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ResolverForm] = {
      SqlParser.get[_root_.java.util.UUID](mappings.userGuid) ~
      SqlParser.str(mappings.uri) map {
        case userGuid ~ uri => {
          com.bryzek.dependency.v0.models.ResolverForm(
            userGuid = userGuid,
            uri = uri
          )
        }
      }
    }

  }

  object Token {

    case class Mappings(
      guid: String = "guid",
      user: io.flow.common.v0.anorm.parsers.Reference.Mappings,
      tag: String = "tag",
      token: String = "token",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        user = io.flow.common.v0.anorm.parsers.Reference.Mappings.prefix(Seq(prefix, "user").filter(!_.isEmpty).mkString("_"), "_"),
        tag = s"${prefix}${sep}tag",
        token = s"${prefix}${sep}token",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Token] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      io.flow.common.v0.anorm.parsers.Reference.parser(mappings.user) ~
      SqlParser.str(mappings.tag) ~
      SqlParser.str(mappings.token) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ user ~ tag ~ token ~ audit => {
          com.bryzek.dependency.v0.models.Token(
            guid = guid,
            user = user,
            tag = tag,
            token = token,
            audit = audit
          )
        }
      }
    }

  }

  object TokenForm {

    case class Mappings(
      userGuid: String = "userGuid",
      tag: String = "tag",
      token: String = "token"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        userGuid = s"${prefix}${sep}user_guid",
        tag = s"${prefix}${sep}tag",
        token = s"${prefix}${sep}token"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.TokenForm] = {
      SqlParser.get[_root_.java.util.UUID](mappings.userGuid) ~
      SqlParser.str(mappings.tag) ~
      SqlParser.str(mappings.token) map {
        case userGuid ~ tag ~ token => {
          com.bryzek.dependency.v0.models.TokenForm(
            userGuid = userGuid,
            tag = tag,
            token = token
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

  object WatchProject {

    case class Mappings(
      guid: String = "guid",
      user: io.flow.common.v0.anorm.parsers.Reference.Mappings,
      project: com.bryzek.dependency.v0.anorm.parsers.Project.Mappings,
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        user = io.flow.common.v0.anorm.parsers.Reference.Mappings.prefix(Seq(prefix, "user").filter(!_.isEmpty).mkString("_"), "_"),
        project = com.bryzek.dependency.v0.anorm.parsers.Project.Mappings.prefix(Seq(prefix, "project").filter(!_.isEmpty).mkString("_"), "_"),
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.WatchProject] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      io.flow.common.v0.anorm.parsers.Reference.parser(mappings.user) ~
      com.bryzek.dependency.v0.anorm.parsers.Project.parser(mappings.project) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ user ~ project ~ audit => {
          com.bryzek.dependency.v0.models.WatchProject(
            guid = guid,
            user = user,
            project = project,
            audit = audit
          )
        }
      }
    }

  }

  object WatchProjectForm {

    case class Mappings(
      userGuid: String = "userGuid",
      projectGuid: String = "projectGuid"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        userGuid = s"${prefix}${sep}user_guid",
        projectGuid = s"${prefix}${sep}project_guid"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.WatchProjectForm] = {
      SqlParser.get[_root_.java.util.UUID](mappings.userGuid) ~
      SqlParser.get[_root_.java.util.UUID](mappings.projectGuid) map {
        case userGuid ~ projectGuid => {
          com.bryzek.dependency.v0.models.WatchProjectForm(
            userGuid = userGuid,
            projectGuid = projectGuid
          )
        }
      }
    }

  }

  object Recommendation {

    case class Mappings(
      libraryRecommendation: com.bryzek.dependency.v0.anorm.parsers.LibraryRecommendation.Mappings,
      binaryRecommendation: com.bryzek.dependency.v0.anorm.parsers.BinaryRecommendation.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        libraryRecommendation = com.bryzek.dependency.v0.anorm.parsers.LibraryRecommendation.Mappings.prefix(prefix, sep),
        binaryRecommendation = com.bryzek.dependency.v0.anorm.parsers.BinaryRecommendation.Mappings.prefix(prefix, sep)
      )
    }

    def parser(mappings: Mappings = Mappings.base): RowParser[com.bryzek.dependency.v0.models.Recommendation] = {
      com.bryzek.dependency.v0.anorm.parsers.LibraryRecommendation.parser(mappings.libraryRecommendation) |
      com.bryzek.dependency.v0.anorm.parsers.BinaryRecommendation.parser(mappings.binaryRecommendation) map {
        case (recommendation) => recommendation
      }
    }

  }

}

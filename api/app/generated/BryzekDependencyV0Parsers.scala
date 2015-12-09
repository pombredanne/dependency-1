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
  object SyncEvent {

    case class Mappings(value: String)

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        value = s"${prefix}${sep}value"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.SyncEvent] = {
      SqlParser.str(mappings.value) map {
        case value => com.bryzek.dependency.v0.models.SyncEvent(value)
      }
    }

  }
  object Visibility {

    case class Mappings(value: String)

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        value = s"${prefix}${sep}value"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Visibility] = {
      SqlParser.str(mappings.value) map {
        case value => com.bryzek.dependency.v0.models.Visibility(value)
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

  object BinarySummary {

    case class Mappings(
      guid: String = "guid",
      name: String = "name"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        name = s"${prefix}${sep}name"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.BinarySummary] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.BinaryType.parser(com.bryzek.dependency.v0.anorm.parsers.BinaryType.Mappings(mappings.name)) map {
        case guid ~ name => {
          com.bryzek.dependency.v0.models.BinarySummary(
            guid = guid,
            name = name
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
      userGuid: String = "user_guid",
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

  object Item {

    case class Mappings(
      guid: String = "guid",
      summary: String = "summary",
      label: String = "label",
      description: String = "description"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        summary = s"${prefix}${sep}summary",
        label = s"${prefix}${sep}label",
        description = s"${prefix}${sep}description"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Item] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      SqlParser.get[com.bryzek.dependency.v0.models.ItemSummary](mappings.summary) ~
      SqlParser.str(mappings.label) ~
      SqlParser.str(mappings.description).? map {
        case guid ~ summary ~ label ~ description => {
          com.bryzek.dependency.v0.models.Item(
            guid = guid,
            summary = summary,
            label = label,
            description = description
          )
        }
      }
    }

  }

  object Library {

    case class Mappings(
      guid: String = "guid",
      groupId: String = "group_id",
      artifactId: String = "artifact_id",
      resolver: com.bryzek.dependency.v0.anorm.parsers.ResolverSummary.Mappings,
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        groupId = s"${prefix}${sep}group_id",
        artifactId = s"${prefix}${sep}artifact_id",
        resolver = com.bryzek.dependency.v0.anorm.parsers.ResolverSummary.Mappings.prefix(Seq(prefix, "resolver").filter(!_.isEmpty).mkString("_"), "_"),
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Library] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      SqlParser.str(mappings.groupId) ~
      SqlParser.str(mappings.artifactId) ~
      com.bryzek.dependency.v0.anorm.parsers.ResolverSummary.parser(mappings.resolver).? ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ groupId ~ artifactId ~ resolver ~ audit => {
          com.bryzek.dependency.v0.models.Library(
            guid = guid,
            groupId = groupId,
            artifactId = artifactId,
            resolver = resolver,
            audit = audit
          )
        }
      }
    }

  }

  object LibraryForm {

    case class Mappings(
      groupId: String = "group_id",
      artifactId: String = "artifact_id",
      resolverGuid: String = "resolver_guid",
      version: com.bryzek.dependency.v0.anorm.parsers.VersionForm.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        groupId = s"${prefix}${sep}group_id",
        artifactId = s"${prefix}${sep}artifact_id",
        resolverGuid = s"${prefix}${sep}resolver_guid",
        version = com.bryzek.dependency.v0.anorm.parsers.VersionForm.Mappings.prefix(Seq(prefix, "version").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LibraryForm] = {
      SqlParser.str(mappings.groupId) ~
      SqlParser.str(mappings.artifactId) ~
      SqlParser.get[_root_.java.util.UUID](mappings.resolverGuid).? ~
      com.bryzek.dependency.v0.anorm.parsers.VersionForm.parser(mappings.version).? map {
        case groupId ~ artifactId ~ resolverGuid ~ version => {
          com.bryzek.dependency.v0.models.LibraryForm(
            groupId = groupId,
            artifactId = artifactId,
            resolverGuid = resolverGuid,
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

  object LibrarySummary {

    case class Mappings(
      guid: String = "guid",
      groupId: String = "group_id",
      artifactId: String = "artifact_id"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        groupId = s"${prefix}${sep}group_id",
        artifactId = s"${prefix}${sep}artifact_id"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.LibrarySummary] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      SqlParser.str(mappings.groupId) ~
      SqlParser.str(mappings.artifactId) map {
        case guid ~ groupId ~ artifactId => {
          com.bryzek.dependency.v0.models.LibrarySummary(
            guid = guid,
            groupId = groupId,
            artifactId = artifactId
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
      crossBuildVersion: String = "cross_build_version",
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
      visibility: String = "visibility",
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
        visibility = s"${prefix}${sep}visibility",
        scms = s"${prefix}${sep}scms",
        name = s"${prefix}${sep}name",
        uri = s"${prefix}${sep}uri",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Project] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.Visibility.parser(com.bryzek.dependency.v0.anorm.parsers.Visibility.Mappings(mappings.visibility)) ~
      com.bryzek.dependency.v0.anorm.parsers.Scms.parser(com.bryzek.dependency.v0.anorm.parsers.Scms.Mappings(mappings.scms)) ~
      SqlParser.str(mappings.name) ~
      SqlParser.str(mappings.uri) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ visibility ~ scms ~ name ~ uri ~ audit => {
          com.bryzek.dependency.v0.models.Project(
            guid = guid,
            visibility = visibility,
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

  object ProjectDetail {

    case class Mappings(
      guid: String = "guid",
      name: String = "name"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        name = s"${prefix}${sep}name"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ProjectDetail] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      SqlParser.str(mappings.name) map {
        case guid ~ name => {
          com.bryzek.dependency.v0.models.ProjectDetail(
            guid = guid,
            name = name
          )
        }
      }
    }

  }

  object ProjectForm {

    case class Mappings(
      name: String = "name",
      visibility: String = "visibility",
      scms: String = "scms",
      uri: String = "uri"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        name = s"${prefix}${sep}name",
        visibility = s"${prefix}${sep}visibility",
        scms = s"${prefix}${sep}scms",
        uri = s"${prefix}${sep}uri"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ProjectForm] = {
      SqlParser.str(mappings.name) ~
      com.bryzek.dependency.v0.anorm.parsers.Visibility.parser(com.bryzek.dependency.v0.anorm.parsers.Visibility.Mappings(mappings.visibility)) ~
      com.bryzek.dependency.v0.anorm.parsers.Scms.parser(com.bryzek.dependency.v0.anorm.parsers.Scms.Mappings(mappings.scms)) ~
      SqlParser.str(mappings.uri) map {
        case name ~ visibility ~ scms ~ uri => {
          com.bryzek.dependency.v0.models.ProjectForm(
            name = name,
            visibility = visibility,
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
      visibility: String = "visibility",
      scms: String = "scms",
      uri: String = "uri"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        name = s"${prefix}${sep}name",
        visibility = s"${prefix}${sep}visibility",
        scms = s"${prefix}${sep}scms",
        uri = s"${prefix}${sep}uri"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ProjectPatchForm] = {
      SqlParser.str(mappings.name).? ~
      com.bryzek.dependency.v0.anorm.parsers.Visibility.parser(com.bryzek.dependency.v0.anorm.parsers.Visibility.Mappings(mappings.visibility)).? ~
      com.bryzek.dependency.v0.anorm.parsers.Scms.parser(com.bryzek.dependency.v0.anorm.parsers.Scms.Mappings(mappings.scms)).? ~
      SqlParser.str(mappings.uri).? map {
        case name ~ visibility ~ scms ~ uri => {
          com.bryzek.dependency.v0.models.ProjectPatchForm(
            name = name,
            visibility = visibility,
            scms = scms,
            uri = uri
          )
        }
      }
    }

  }

  object ProjectSummary {

    case class Mappings(
      guid: String = "guid",
      name: String = "name"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        name = s"${prefix}${sep}name"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ProjectSummary] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      SqlParser.str(mappings.name) map {
        case guid ~ name => {
          com.bryzek.dependency.v0.models.ProjectSummary(
            guid = guid,
            name = name
          )
        }
      }
    }

  }

  object Recommendation {

    case class Mappings(
      guid: String = "guid",
      project: com.bryzek.dependency.v0.anorm.parsers.ProjectDetail.Mappings,
      `type`: String = "type",
      `object`: io.flow.common.v0.anorm.parsers.Reference.Mappings,
      name: String = "name",
      from: String = "from",
      to: String = "to",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        project = com.bryzek.dependency.v0.anorm.parsers.ProjectDetail.Mappings.prefix(Seq(prefix, "project").filter(!_.isEmpty).mkString("_"), "_"),
        `type` = s"${prefix}${sep}type",
        `object` = io.flow.common.v0.anorm.parsers.Reference.Mappings.prefix(Seq(prefix, "object").filter(!_.isEmpty).mkString("_"), "_"),
        name = s"${prefix}${sep}name",
        from = s"${prefix}${sep}from",
        to = s"${prefix}${sep}to",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Recommendation] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.ProjectDetail.parser(mappings.project) ~
      com.bryzek.dependency.v0.anorm.parsers.RecommendationType.parser(com.bryzek.dependency.v0.anorm.parsers.RecommendationType.Mappings(mappings.`type`)) ~
      io.flow.common.v0.anorm.parsers.Reference.parser(mappings.`object`) ~
      SqlParser.str(mappings.name) ~
      SqlParser.str(mappings.from) ~
      SqlParser.str(mappings.to) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ project ~ typeInstance ~ objectInstance ~ name ~ from ~ to ~ audit => {
          com.bryzek.dependency.v0.models.Recommendation(
            guid = guid,
            project = project,
            `type` = typeInstance,
            `object` = objectInstance,
            name = name,
            from = from,
            to = to,
            audit = audit
          )
        }
      }
    }

  }

  object Repository {

    case class Mappings(
      name: String = "name",
      visibility: String = "visibility",
      uri: String = "uri"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        name = s"${prefix}${sep}name",
        visibility = s"${prefix}${sep}visibility",
        uri = s"${prefix}${sep}uri"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Repository] = {
      SqlParser.str(mappings.name) ~
      com.bryzek.dependency.v0.anorm.parsers.Visibility.parser(com.bryzek.dependency.v0.anorm.parsers.Visibility.Mappings(mappings.visibility)) ~
      SqlParser.str(mappings.uri) map {
        case name ~ visibility ~ uri => {
          com.bryzek.dependency.v0.models.Repository(
            name = name,
            visibility = visibility,
            uri = uri
          )
        }
      }
    }

  }

  object Resolver {

    case class Mappings(
      guid: String = "guid",
      visibility: String = "visibility",
      user: io.flow.common.v0.anorm.parsers.Reference.Mappings,
      uri: String = "uri",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        visibility = s"${prefix}${sep}visibility",
        user = io.flow.common.v0.anorm.parsers.Reference.Mappings.prefix(Seq(prefix, "user").filter(!_.isEmpty).mkString("_"), "_"),
        uri = s"${prefix}${sep}uri",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Resolver] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.Visibility.parser(com.bryzek.dependency.v0.anorm.parsers.Visibility.Mappings(mappings.visibility)) ~
      io.flow.common.v0.anorm.parsers.Reference.parser(mappings.user) ~
      SqlParser.str(mappings.uri) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ visibility ~ user ~ uri ~ audit => {
          com.bryzek.dependency.v0.models.Resolver(
            guid = guid,
            visibility = visibility,
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
      visibility: String = "visibility",
      userGuid: String = "user_guid",
      uri: String = "uri",
      credentials: String = "credentials"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        visibility = s"${prefix}${sep}visibility",
        userGuid = s"${prefix}${sep}user_guid",
        uri = s"${prefix}${sep}uri",
        credentials = s"${prefix}${sep}credentials"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ResolverForm] = {
      com.bryzek.dependency.v0.anorm.parsers.Visibility.parser(com.bryzek.dependency.v0.anorm.parsers.Visibility.Mappings(mappings.visibility)) ~
      SqlParser.get[_root_.java.util.UUID](mappings.userGuid) ~
      SqlParser.str(mappings.uri) ~
      SqlParser.get[com.bryzek.dependency.v0.models.Credentials](mappings.credentials).? map {
        case visibility ~ userGuid ~ uri ~ credentials => {
          com.bryzek.dependency.v0.models.ResolverForm(
            visibility = visibility,
            userGuid = userGuid,
            uri = uri,
            credentials = credentials
          )
        }
      }
    }

  }

  object ResolverSummary {

    case class Mappings(
      guid: String = "guid",
      visibility: String = "visibility",
      uri: String = "uri"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        visibility = s"${prefix}${sep}visibility",
        uri = s"${prefix}${sep}uri"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.ResolverSummary] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.Visibility.parser(com.bryzek.dependency.v0.anorm.parsers.Visibility.Mappings(mappings.visibility)) ~
      SqlParser.str(mappings.uri) map {
        case guid ~ visibility ~ uri => {
          com.bryzek.dependency.v0.models.ResolverSummary(
            guid = guid,
            visibility = visibility,
            uri = uri
          )
        }
      }
    }

  }

  object Sync {

    case class Mappings(
      guid: String = "guid",
      event: String = "event",
      audit: io.flow.common.v0.anorm.parsers.Audit.Mappings
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        guid = s"${prefix}${sep}guid",
        event = s"${prefix}${sep}event",
        audit = io.flow.common.v0.anorm.parsers.Audit.Mappings.prefix(Seq(prefix, "audit").filter(!_.isEmpty).mkString("_"), "_")
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.Sync] = {
      SqlParser.get[_root_.java.util.UUID](mappings.guid) ~
      com.bryzek.dependency.v0.anorm.parsers.SyncEvent.parser(com.bryzek.dependency.v0.anorm.parsers.SyncEvent.Mappings(mappings.event)) ~
      io.flow.common.v0.anorm.parsers.Audit.parser(mappings.audit) map {
        case guid ~ event ~ audit => {
          com.bryzek.dependency.v0.models.Sync(
            guid = guid,
            event = event,
            audit = audit
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
      userGuid: String = "user_guid",
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

  object UsernamePassword {

    case class Mappings(
      username: String = "username",
      password: String = "password"
    )

    object Mappings {

      val base = prefix("", "")

      def table(table: String) = prefix(table, ".")

      def prefix(prefix: String, sep: String) = Mappings(
        username = s"${prefix}${sep}username",
        password = s"${prefix}${sep}password"
      )

    }

    def table(table: String) = parser(Mappings.prefix(table, "."))

    def parser(mappings: Mappings): RowParser[com.bryzek.dependency.v0.models.UsernamePassword] = {
      SqlParser.str(mappings.username) ~
      SqlParser.str(mappings.password).? map {
        case username ~ password => {
          com.bryzek.dependency.v0.models.UsernamePassword(
            username = username,
            password = password
          )
        }
      }
    }

  }

  object VersionForm {

    case class Mappings(
      version: String = "version",
      crossBuildVersion: String = "cross_build_version"
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
      userGuid: String = "user_guid",
      projectGuid: String = "project_guid"
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

}
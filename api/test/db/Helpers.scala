package db

import io.flow.play.clients.MockUserClient
import com.bryzek.dependency.v0.models._
import io.flow.user.v0.models.{NameForm, User, UserForm}
import java.util.UUID
import scala.util.Random

trait Helpers {

  lazy val systemUser = createUser()

  def createTestEmail(): String = {
    s"${createTestKey}@test.bryzek.com"
  }

  def createTestName(): String = {
    s"Z Test ${UUID.randomUUID}"
  }

  def createTestKey(): String = {
    s"z-test-${UUID.randomUUID.toString.toLowerCase}"
  }

  def create[T](result: Either[Seq[String], T]): T = {
    result match {
      case Left(errors) => sys.error(errors.mkString(", "))
      case Right(obj) => obj
    }
  }

  /**
    * Function called on each iteration until it returns true, up
    * until maxAttempts (at which point an error is raised)
    */
  def waitFor(
    function: () => Boolean,
    maxAttempts: Int = 25,
    msBetweenAttempts: Int = 250
  ): Boolean = {
    var ctr = 0
    var found = false
    while (!found) {
      found = function()
      ctr += 1
      if (ctr > maxAttempts) {
        sys.error("Did not create user organization")
      }
      Thread.sleep(msBetweenAttempts)
    }
    true
  }

  @scala.annotation.tailrec
  final def positiveRandomLong(): Long = {
    val value = (new Random()).nextLong
    (value > 0) match {
      case true => value
      case false => positiveRandomLong()
    }
  }

  def createOrganization(
    form: OrganizationForm = createOrganizationForm(),
    user: User = systemUser
  ): Organization = {
    OrganizationsDao.create(user, form).right.getOrElse {
      sys.error("Failed to create organization")
    }
  }

  def createOrganizationForm() = {
    OrganizationForm(
      key = createTestKey()
    )
  }

  def createBinary(
    org: Organization = createOrganization()
  ) (
    implicit form: BinaryForm = createBinaryForm(org)
  ): Binary = {
    BinariesDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create binary")
    }
  }

  def createBinaryForm(
    org: Organization = createOrganization()
  ) = BinaryForm(
    organizationGuid = org.guid,
    name = BinaryType.UNDEFINED(s"z-test-binary-${UUID.randomUUID}".toLowerCase)
  )

  def createBinaryVersion(
    org: Organization = createOrganization()
  ) (
    implicit binary: Binary = createBinary(org),
             version: String = s"0.0.1-${UUID.randomUUID}".toLowerCase
  ): BinaryVersion = {
    BinaryVersionsDao.create(systemUser, binary.guid, version)
  }

  def createLibrary(
    org: Organization = createOrganization(),
    user: User = systemUser
  ) (
    implicit form: LibraryForm = createLibraryForm(org, user)
  ): Library = {
    LibrariesDao.create(user, form).right.getOrElse {
      sys.error("Failed to create library")
    }
  }

  def createLibraryForm(
    org: Organization = createOrganization(),
    user: User = systemUser
  ) (
    implicit versionForm: VersionForm = VersionForm("0.0.1"),
             resolver: Resolver = createResolver(org, user)
  ) = LibraryForm(
    organizationGuid = org.guid,
    groupId = s"z-test.${UUID.randomUUID}".toLowerCase,
    artifactId = s"z-test-${UUID.randomUUID}".toLowerCase,
    version = Some(versionForm),
    resolverGuid = resolver.guid
  )

  def createLibraryVersion(
    org: Organization = createOrganization(),
    user: User = systemUser
  ) (
    implicit library: Library = createLibrary(org, user),
             version: VersionForm = createVersionForm()
  ): LibraryVersion = {
    LibraryVersionsDao.create(user, library.guid, version)
  }

  def createVersionForm(
    version: String = s"0.0.1-${UUID.randomUUID}".toLowerCase,
    crossBuildVersion: Option[String] = None
  ) = {
    VersionForm(version, crossBuildVersion)
  }

  def createProject(
    org: Organization = createOrganization()
  ) (
    implicit form: ProjectForm = createProjectForm(org)
  ): Project = {
    val user = OrganizationsDao.findByKey(Authorization.All, form.organization).flatMap { org =>
      UsersDao.findById(org.audit.createdBy.id)
    }.getOrElse {
      sys.error("Could not find user that created org")
    }

    create(ProjectsDao.create(user, form))
  }

  def createProjectForm(
    org: Organization = createOrganization()
  ) = {
    ProjectForm(
      organization = org.key,
      name = createTestName(),
      visibility = Visibility.Private,
      scms = Scms.Github,
      uri = s"http://github.com/test/${UUID.randomUUID}"
    )
  }

  def createProjectWithLibrary(
    org: Organization = createOrganization(),
    version: VersionForm = VersionForm(version = "0.0.1")
  ) (
    implicit libraryForm: LibraryForm = createLibraryForm(org).copy(
      groupId = s"z-test-${UUID.randomUUID}".toLowerCase,
      artifactId = s"z-test-${UUID.randomUUID}".toLowerCase
    )
  ): (Project, LibraryVersion) = {
    val project = createProject(org)
    val library = createLibrary(org)(libraryForm)

    val projectLibrary = createProjectLibrary(project)(
      createProjectLibraryForm(
        project,
        groupId = library.groupId,
        artifactId = library.artifactId,
        version = version.version,
        crossBuildVersion = version.crossBuildVersion
      )
    )

    val libraryVersion = LibraryVersionsDao.upsert(systemUser, library.guid, version)

    ProjectLibrariesDao.setLibrary(systemUser, projectLibrary, library)

    (project, libraryVersion)
  }

  def createProjectWithBinary(
    org: Organization = createOrganization()
  ): (Project, BinaryVersion) = {
    val binary = createBinary(org)
    val binaryVersion = createBinaryVersion(org)(binary = binary)
    val project = createProject(org)

    val projectBinary = create(ProjectBinariesDao.create(
      systemUser,
      createProjectBinaryForm(
        project = project,
        name = binary.name,
        version = binaryVersion.version
      )
    ))

    ProjectBinariesDao.setBinary(systemUser, projectBinary, binary)

    (project, binaryVersion)
  }

  def makeUser(
    form: UserForm = makeUserForm()
  ): User = {
    MockUserClient.makeUser(form)
  }

  def makeUserForm() = UserForm(
    email = None,
    name = None,
    avatarUrl = None
  )

  def createUser(
    form: UserForm = createUserForm()
  ): User = {
    create(UsersDao.create(None, form))
  }

  def createUserForm(
    email: String = createTestEmail(),
    name: Option[NameForm] = None
  ) = UserForm(
    email = Some(email),
    name = name
  )

  def createGithubUser(
    form: GithubUserForm = createGithubUserForm()
  ): GithubUser = {
    GithubUsersDao.create(None, form)
  }

  def createGithubUserForm(
    user: User = createUser(),
    id: Long = positiveRandomLong(),
    login: String = createTestEmail()
  ) = {
    GithubUserForm(
      userGuid = user.id,
      id = id,
      login = login
    )
  }

  def createToken(
    form: TokenForm = createTokenForm()
  ): Token = {
    TokensDao.create(systemUser, form)
  }

  def createTokenForm(
    user: User = createUser(),
    tag: String = createTestName().toLowerCase,
    token: String = UUID.randomUUID().toString.toLowerCase
  ) = {
    TokenForm(
      userGuid = user.id,
      tag = tag,
      token = token
    )
  }

  def createSync(
    form: SyncForm = createSyncForm()
  ): Sync = {
    SyncsDao.create(systemUser, form)
  }

  def createSyncForm(
    `type`: String = "test",
    objectGuid: UUID = UUID.randomUUID,
    event: SyncEvent = SyncEvent.Started
  ) = {
    SyncForm(
      `type` = `type`,
      objectGuid = objectGuid,
      event = event
    )
  }

  def createResolver(
    org: Organization,
    user: User = systemUser
  ) (
    implicit form: ResolverForm = createResolverForm(org)
  ): Resolver = {
    create(ResolversDao.create(user, form))
  }

  def createResolverForm(
    org: Organization = createOrganization(),
    visibility: Visibility = Visibility.Private,
    uri: String = s"http://${UUID.randomUUID}.z-test.flow.io"
  ) = {
    ResolverForm(
      visibility = visibility,
      organization = org.key,
      uri = uri
    )
  }

  def createMembership(
    form: MembershipForm = createMembershipForm()
  ): Membership = {
    create(MembershipsDao.create(systemUser, form))
  }

  def createMembershipForm(
    org: Organization = createOrganization(),
    user: User = createUser(),
    role: Role = Role.Member
  ) = {
    MembershipForm(
      organization = org.key,
      userGuid = user.id,
      role = role
    )
  }

  def createWatchProject(
    org: Organization
  ) (
    implicit form: WatchProjectForm = createWatchProjectForm(org)
  ): WatchProject = {
    create(WatchProjectsDao.create(systemUser, form))
  }

  def createWatchProjectForm(
    org: Organization
  ) (
    implicit user: User = createUser(),
             project: Project = createProject(org)
  ) = {
    WatchProjectForm(
      userGuid = user.id,
      projectGuid = project.guid
    )
  }

  def createLibraryWithMultipleVersions(
    org: Organization
  ) (
    implicit versions: Seq[String] = Seq("1.0.0", "1.0.1", "1.0.2")
  ): (Library, Seq[LibraryVersion]) = {
    val library = createLibrary(org)(createLibraryForm(org).copy(version = None))
    (
      library,
      versions.map { version =>
        createLibraryVersion(
          org
        ) (
          library = library,
          version = VersionForm(version = version)
        )
      }
    )
  }

  def addLibraryVersion(project: Project, libraryVersion: LibraryVersion) {
    val projectLibrary = create(
      ProjectLibrariesDao.upsert(
        systemUser,
        ProjectLibraryForm(
          projectGuid = project.guid,
          groupId = libraryVersion.library.groupId,
          artifactId = libraryVersion.library.artifactId,
          path = "test.sbt",
          version = VersionForm(libraryVersion.version, libraryVersion.crossBuildVersion)
        )
      )
    )

    ProjectLibrariesDao.setLibrary(systemUser, projectLibrary, libraryVersion.library)
  }

  def upsertItem(
    org: Organization
  ) (
    implicit form: ItemForm = createItemForm(org)
  ): Item = {
    ItemsDao.upsert(systemUser, form)
  }

  def createItemSummary(
    org: Organization
  ) (
    implicit binary: Binary = createBinary(org)
  ): ItemSummary = {
    BinarySummary(
      guid = binary.guid,
      organization = OrganizationSummary(org.guid, org.key),
      name = binary.name
    )
  }

  def createItemForm(
    org: Organization
  ) (
    implicit summary: ItemSummary = createItemSummary(org)
  ): ItemForm = {
    val label = summary match {
      case BinarySummary(guid, org, name) => name.toString
      case LibrarySummary(guid, org, groupId, artifactId) => Seq(groupId, artifactId).mkString(".")
      case ProjectSummary(guid, org, name) => name
      case ItemSummaryUndefinedType(name) => name
    }
    ItemForm(
      summary = summary,
      label = label,
      description = None,
      contents = label
    )
  }

  def createSubscription(
    form: SubscriptionForm = createSubscriptionForm()
  ): Subscription = {
    create(SubscriptionsDao.create(systemUser, form))
  }

  def createSubscriptionForm(
    user: User = createUser(),
    publication: Publication = Publication.DailySummary
  ) = {
    SubscriptionForm(
      userGuid = user.id,
      publication = publication
    )
  }

  def createLastEmail(
    form: LastEmailForm = createLastEmailForm()
  ): LastEmail = {
    LastEmailsDao.record(systemUser, form)
  }

  def createLastEmailForm(
    user: User = createUser(),
    publication: Publication = Publication.DailySummary
  ) = LastEmailForm(
    userGuid = user.id,
    publication = publication
  )

  def createProjectLibrary(
    project: Project = createProject()
  ) (
    implicit form: ProjectLibraryForm = createProjectLibraryForm(project)
  ): ProjectLibrary = {
    create(ProjectLibrariesDao.create(systemUser, form))
  }

  def createProjectLibraryForm(
    project: Project = createProject(),
    groupId: String = s"z-test.${UUID.randomUUID}".toLowerCase,
    artifactId: String = s"z-test-${UUID.randomUUID}".toLowerCase,
    path: String = "build.sbt",
    version: String = "0.0.1",
    crossBuildVersion: Option[String] = None
  ) = {
    ProjectLibraryForm(
      projectGuid = project.guid,
      groupId = groupId,
      artifactId = artifactId,
      path = path,
      version = VersionForm(version, crossBuildVersion)
    )
  }

  def createProjectBinary(
    project: Project = createProject()
  ) (
    implicit form: ProjectBinaryForm = createProjectBinaryForm(project)
  ): ProjectBinary = {
    create(ProjectBinariesDao.create(systemUser, form))
  }

  def createProjectBinaryForm(
    project: Project = createProject(),
    name: BinaryType = BinaryType.UNDEFINED(createTestKey()),
    path: String = "build.sbt",
    version: String = "0.0.1"
  ) = {
    ProjectBinaryForm(
      projectGuid = project.guid,
      name = name,
      path = path,
      version = version
    )
  }

}

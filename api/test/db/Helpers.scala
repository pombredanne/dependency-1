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
    form: BinaryForm = createBinaryForm(org)
  ): Binary = {
    BinariesDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create binary")
    }
  }

  def createBinaryForm(
    org: Organization = createOrganization()
  ) = BinaryForm(
    organizationGuid = org.guid,
    name = s"z-test-binary-${UUID.randomUUID}".toLowerCase,
    version = "0.0.1"
  )

  def createBinaryVersion(
    org: Organization = createOrganization()
  ) (
    binary: Binary = createBinary(org)(),
    version: String = s"0.0.1-${UUID.randomUUID}".toLowerCase
  ): BinaryVersion = {
    BinaryVersionsDao.create(systemUser, binary.guid, version)
  }

  def createLibrary(
    org: Organization = createOrganization()
  ) (
    form: LibraryForm = createLibraryForm(org)()
  ): Library = {
    LibrariesDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create library")
    }
  }

  def createLibraryForm(
    org: Organization = createOrganization()
  ) (
    versionForm: VersionForm = VersionForm("0.0.1")
  ) = LibraryForm(
    organizationGuid = org.guid,
    groupId = s"z-test.${UUID.randomUUID}".toLowerCase,
    artifactId = s"z-test-${UUID.randomUUID}".toLowerCase,
    version = Some(versionForm)
  )

  def createLibraryVersion(
    org: Organization = createOrganization()
  ) (
    library: Library = createLibrary(org)(),
    version: VersionForm = createVersionForm()
  ): LibraryVersion = {
    LibraryVersionsDao.create(systemUser, library.guid, version)
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
    val user = OrganizationsDao.findByGuid(Authorization.All, form.organizationGuid).flatMap { org =>
      UsersDao.findByGuid(org.audit.createdBy.guid)
    }.getOrElse {
      sys.error("Could not find user that created org")
    }

    ProjectsDao.create(user, form) match {
      case Left(errors) => sys.error("Failed to create project: " + errors.mkString(", "))
      case Right(project) => project
    }
  }

  def createProjectForm(
    org: Organization = createOrganization()
  ) = {
    ProjectForm(
      organizationGuid = org.guid,
      name = createTestName(),
      visibility = Visibility.Private,
      scms = Scms.Github,
      uri = s"http://github.com/test/${UUID.randomUUID}"
    )
  }

  def createProjectWithLibrary(
    org: Organization = createOrganization()
  ) (
    libraryForm: LibraryForm = createLibraryForm(org)().copy(
      groupId = s"z-test-${UUID.randomUUID}".toLowerCase,
      artifactId = s"z-test-${UUID.randomUUID}".toLowerCase,
      version = Some(createVersionForm())
    )
  ): (Project, LibraryVersion) = {
    val project = createProject(org)()
    ProjectsDao.setDependencies(systemUser, project, libraries = Some(Seq(libraryForm)))

    val library = LibrariesDao.findByOrganizationGuidAndGroupIdAndArtifactId(
      org.guid, libraryForm.groupId, libraryForm.artifactId
    ).getOrElse {
      sys.error("Failed to find library")
    }

    val libraryVersion = LibraryVersionsDao.findByLibraryAndVersionAndCrossBuildVersion(
      library, libraryForm.version.get.version, libraryForm.version.get.crossBuildVersion
    ).getOrElse {
      sys.error("Failed to find library version")
    }

    (project, libraryVersion)
  }

  def createProjectWithBinary(
    org: Organization = createOrganization()
  ): (Project, BinaryVersion) = {
    val binaryForm = createBinaryForm(org).copy(
      name = createTestName(),
      version = UUID.randomUUID.toString
    )

    val project = createProject(org)()
    ProjectsDao.setDependencies(systemUser, project, binaries = Some(Seq(binaryForm)))

    val binary = BinariesDao.findByName(Authorization.All, binaryForm.name).getOrElse {
      sys.error("Failed to find binary")
    }

    val binaryVersion = BinaryVersionsDao.findByBinaryAndVersion(binary, binaryForm.version).getOrElse {
      sys.error("Failed to find binary version")
    }

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
    UsersDao.create(None, form).right.getOrElse {
      sys.error("Failed to create user")
    }
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
      userGuid = user.guid,
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
      userGuid = user.guid,
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
    objectGuid: UUID = UUID.randomUUID,
    event: SyncEvent = SyncEvent.Started
  ) = {
    SyncForm(
      objectGuid = objectGuid,
      event = event
    )
  }

  def createResolver(
    form: ResolverForm = createResolverForm()
  ): Resolver = {
    ResolversDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create resolver")
    }
  }

  def createResolverForm(
    org: Organization = createOrganization(),
    visibility: Visibility = Visibility.Private,
    uri: String = s"http://${UUID.randomUUID}.z-test.flow.io"
  ) = {
    ResolverForm(
      visibility = visibility,
      organizationGuid = org.guid,
      uri = uri
    )
  }

  def createMembership(
    form: MembershipForm = createMembershipForm()
  ): Membership = {
    MembershipsDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create membership")
    }
  }

  def createMembershipForm(
    org: Organization = createOrganization(),
    user: User = createUser(),
    role: Role = Role.Member
  ) = {
    MembershipForm(
      organizationGuid = org.guid,
      userGuid = user.guid,
      role = role
    )
  }

  def createWatchProject(
    org: Organization
  ) (
    form: WatchProjectForm = createWatchProjectForm(org)()
  ): WatchProject = {
    WatchProjectsDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create watch project")
    }
  }

  def createWatchProjectForm(
    org: Organization
  ) (
    user: User = createUser(),
    project: Project = createProject(org)()
  ) = {
    WatchProjectForm(
      userGuid = user.guid,
      projectGuid = project.guid
    )
  }

  def createLibraryWithMultipleVersions(
    org: Organization
  ) (
    versions: Seq[String] = Seq("1.0.0", "1.0.1", "1.0.2")
  ): Seq[LibraryVersion] = {
    val library = createLibrary(org)(createLibraryForm(org)().copy(version = None))
    versions.map { version =>
      createLibraryVersion(
        org
      ) (
        library = library,
        version = VersionForm(version = version)
      )
    }
  }

  def addLibraryVersion(project: Project, libraryVersion: LibraryVersion) {
    ProjectsDao.setDependencies(
      systemUser,
      project,
      libraries = Some(
        Seq(
          LibraryForm(
            organizationGuid = project.organization.guid,
            groupId = libraryVersion.library.groupId,
            artifactId = libraryVersion.library.artifactId,
            version = Some(VersionForm(version = libraryVersion.version))
          )
        )
      )
    )
  }

  def upsertItem(
    org: Organization
  ) (
    form: ItemForm = createItemForm(org)()
  ): Item = {
    ItemsDao.upsert(systemUser, form)
  }

  def createItemSummary(
    org: Organization
  ) (
    binary: Binary = createBinary(org)()
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
    summary: ItemSummary = createItemSummary(org)()
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
    SubscriptionsDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create subscription")
    }
  }

  def createSubscriptionForm(
    user: User = createUser(),
    publication: Publication = Publication.DailySummary
  ) = {
    SubscriptionForm(
      userGuid = user.guid,
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
    userGuid = user.guid,
    publication = publication
  )

}

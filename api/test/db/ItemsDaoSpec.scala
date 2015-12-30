package db

import com.bryzek.dependency.v0.models.{BinarySummary, LibrarySummary, OrganizationSummary, ProjectSummary, Visibility}
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ItemsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "upsert" in {
    val form = createItemForm(org)()
    val item1 = ItemsDao.create(systemUser, form)

    val item2 = ItemsDao.upsert(systemUser, form)
    item1.guid must be(item2.guid)

    val item3 = upsertItem(org)()

    item1.guid must be(item2.guid)
    item2.guid must not be(item3.guid)
    item2.label must be(form.label)
  }

  "findByGuid" in {
    val item = upsertItem(org)()
    ItemsDao.findByGuid(Authorization.All, item.guid).map(_.guid) must be(
      Some(item.guid)
    )

    ItemsDao.findByGuid(Authorization.All, UUID.randomUUID) must be(None)
  }

  "findByObjectGuid" in {
    val binary = createBinary(org)()
    val item = ItemsDao.upsertBinary(systemUser, binary)
    ItemsDao.findByObjectGuid(Authorization.All, binary.guid).map(_.guid) must be(
      Some(item.guid)
    )

    ItemsDao.findByObjectGuid(Authorization.All, UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val item1 = upsertItem(org)()
    val item2 = upsertItem(org)()

    ItemsDao.findAll(Authorization.All, guids = Some(Seq(item1.guid, item2.guid))).map(_.guid).sorted must be(
      Seq(item1.guid, item2.guid).sorted
    )

    ItemsDao.findAll(Authorization.All, guids = Some(Nil)) must be(Nil)
    ItemsDao.findAll(Authorization.All, guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    ItemsDao.findAll(Authorization.All, guids = Some(Seq(item1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(item1.guid))
  }

  "supports binaries" in {
    val binary = createBinary(org)()
    val itemBinary = ItemsDao.upsertBinary(systemUser, binary)

    val actual = ItemsDao.findByObjectGuid(Authorization.All, binary.guid).getOrElse {
      sys.error("Failed to create binary")
    }
    actual.label must be(binary.name.toString)
    actual.summary must be(
      BinarySummary(
        guid = binary.guid,
        organization = OrganizationSummary(org.guid, org.key),
        name = binary.name
      )
    )

    ItemsDao.findAll(Authorization.All, q = Some(binary.guid.toString)).headOption.map(_.guid) must be(Some(actual.guid))
    ItemsDao.findAll(Authorization.All, q = Some(UUID.randomUUID.toString)) must be(Nil)
  }

  "supports libraries" in {
    val library = createLibrary(org)()

    val itemLibrary = ItemsDao.upsertLibrary(systemUser, library)
    val actual = ItemsDao.findByObjectGuid(Authorization.All, library.guid).getOrElse {
      sys.error("Failed to create library")
    }
    actual.label must be(Seq(library.groupId, library.artifactId).mkString("."))
    actual.summary must be(
      LibrarySummary(
        guid = library.guid,
        organization = OrganizationSummary(org.guid, org.key),
        groupId = library.groupId,
        artifactId = library.artifactId
      )
    )

    ItemsDao.findAll(Authorization.All, q = Some(library.guid.toString)).headOption.map(_.guid) must be(Some(actual.guid))
    ItemsDao.findAll(Authorization.All, q = Some(UUID.randomUUID.toString)) must be(Nil)
  }

  "supports projects" in {
    val project = createProject(org)

    val itemProject = ItemsDao.upsertProject(systemUser, project)
    val actual = ItemsDao.findByObjectGuid(Authorization.All, project.guid).getOrElse {
      sys.error("Failed to create project")
    }
    actual.label must be(project.name)
    actual.summary must be(
      ProjectSummary(
        guid = project.guid,
        organization = OrganizationSummary(org.guid, org.key),
        name = project.name
      )
    )

    ItemsDao.findAll(Authorization.All, q = Some(project.guid.toString)).headOption.map(_.guid) must be(Some(actual.guid))
    ItemsDao.findAll(Authorization.All, q = Some(UUID.randomUUID.toString)) must be(Nil)
  }

  "authorization for public projects" in {
    val user = createUser()
    val org = createOrganization(user = user)
    val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Public))
    val item = ItemsDao.upsertProject(systemUser, project)

    ItemsDao.findAll(Authorization.PublicOnly, objectGuid = Some(project.guid)).map(_.guid) must be(Seq(item.guid))
    ItemsDao.findAll(Authorization.All, objectGuid = Some(project.guid)).map(_.guid) must be(Seq(item.guid))
    ItemsDao.findAll(Authorization.Organization(org.guid), objectGuid = Some(project.guid)).map(_.guid) must be(Seq(item.guid))
    ItemsDao.findAll(Authorization.Organization(createOrganization().guid), objectGuid = Some(project.guid)).map(_.guid) must be(Seq(item.guid))
    ItemsDao.findAll(Authorization.User(user.id), objectGuid = Some(project.guid)).map(_.guid) must be(Seq(item.guid))
  }

  "authorization for private projects" in {
    val user = createUser()
    val org = createOrganization(user = user)
    val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Private))
    val item = ItemsDao.upsertProject(systemUser, project)

    ItemsDao.findAll(Authorization.PublicOnly, objectGuid = Some(project.guid)) must be(Nil)
    ItemsDao.findAll(Authorization.All, objectGuid = Some(project.guid)).map(_.guid) must be(Seq(item.guid))
    ItemsDao.findAll(Authorization.Organization(org.guid), objectGuid = Some(project.guid)).map(_.guid) must be(Seq(item.guid))
    ItemsDao.findAll(Authorization.Organization(createOrganization().guid), objectGuid = Some(project.guid)) must be(Nil)
    ItemsDao.findAll(Authorization.User(user.id), objectGuid = Some(project.guid)).map(_.guid) must be(Seq(item.guid))
    ItemsDao.findAll(Authorization.User(createUser().guid), objectGuid = Some(project.guid)) must be(Nil)
  }

}


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
    item1.id must be(item2.id)

    val item3 = upsertItem(org)()

    item1.id must be(item2.id)
    item2.id must not be(item3.id)
    item2.label must be(form.label)
  }

  "findById" in {
    val item = upsertItem(org)()
    ItemsDao.findById(Authorization.All, item.id).map(_.id) must be(
      Some(item.id)
    )

    ItemsDao.findById(Authorization.All, UUID.randomUUID) must be(None)
  }

  "findByObjectId" in {
    val binary = createBinary(org)()
    val item = ItemsDao.upsertBinary(systemUser, binary)
    ItemsDao.findByObjectId(Authorization.All, binary.id).map(_.id) must be(
      Some(item.id)
    )

    ItemsDao.findByObjectId(Authorization.All, UUID.randomUUID) must be(None)
  }

  "findAll by ids" in {
    val item1 = upsertItem(org)()
    val item2 = upsertItem(org)()

    ItemsDao.findAll(Authorization.All, ids = Some(Seq(item1.id, item2.id))).map(_.id).sorted must be(
      Seq(item1.id, item2.id).sorted
    )

    ItemsDao.findAll(Authorization.All, ids = Some(Nil)) must be(Nil)
    ItemsDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID))) must be(Nil)
    ItemsDao.findAll(Authorization.All, ids = Some(Seq(item1.id, UUID.randomUUID))).map(_.id) must be(Seq(item1.id))
  }

  "supports binaries" in {
    val binary = createBinary(org)()
    val itemBinary = ItemsDao.upsertBinary(systemUser, binary)

    val actual = ItemsDao.findByObjectId(Authorization.All, binary.id).getOrElse {
      sys.error("Failed to create binary")
    }
    actual.label must be(binary.name.toString)
    actual.summary must be(
      BinarySummary(
        id = binary.id,
        organization = OrganizationSummary(org.id, org.key),
        name = binary.name
      )
    )

    ItemsDao.findAll(Authorization.All, q = Some(binary.id.toString)).headOption.map(_.id) must be(Some(actual.id))
    ItemsDao.findAll(Authorization.All, q = Some(UUID.randomUUID.toString)) must be(Nil)
  }

  "supports libraries" in {
    val library = createLibrary(org)()

    val itemLibrary = ItemsDao.upsertLibrary(systemUser, library)
    val actual = ItemsDao.findByObjectId(Authorization.All, library.id).getOrElse {
      sys.error("Failed to create library")
    }
    actual.label must be(Seq(library.groupId, library.artifactId).mkString("."))
    actual.summary must be(
      LibrarySummary(
        id = library.id,
        organization = OrganizationSummary(org.id, org.key),
        groupId = library.groupId,
        artifactId = library.artifactId
      )
    )

    ItemsDao.findAll(Authorization.All, q = Some(library.id.toString)).headOption.map(_.id) must be(Some(actual.id))
    ItemsDao.findAll(Authorization.All, q = Some(UUID.randomUUID.toString)) must be(Nil)
  }

  "supports projects" in {
    val project = createProject(org)

    val itemProject = ItemsDao.upsertProject(systemUser, project)
    val actual = ItemsDao.findByObjectId(Authorization.All, project.id).getOrElse {
      sys.error("Failed to create project")
    }
    actual.label must be(project.name)
    actual.summary must be(
      ProjectSummary(
        id = project.id,
        organization = OrganizationSummary(org.id, org.key),
        name = project.name
      )
    )

    ItemsDao.findAll(Authorization.All, q = Some(project.id.toString)).headOption.map(_.id) must be(Some(actual.id))
    ItemsDao.findAll(Authorization.All, q = Some(UUID.randomUUID.toString)) must be(Nil)
  }

  "authorization for public projects" in {
    val user = createUser()
    val org = createOrganization(user = user)
    val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Public))
    val item = ItemsDao.upsertProject(systemUser, project)

    ItemsDao.findAll(Authorization.PublicOnly, objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.All, objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.Organization(org.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.Organization(createOrganization().id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.User(user.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
  }

  "authorization for private projects" in {
    val user = createUser()
    val org = createOrganization(user = user)
    val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Private))
    val item = ItemsDao.upsertProject(systemUser, project)

    ItemsDao.findAll(Authorization.PublicOnly, objectId = Some(project.id)) must be(Nil)
    ItemsDao.findAll(Authorization.All, objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.Organization(org.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.Organization(createOrganization().id), objectId = Some(project.id)) must be(Nil)
    ItemsDao.findAll(Authorization.User(user.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.User(createUser().id), objectId = Some(project.id)) must be(Nil)
  }

}


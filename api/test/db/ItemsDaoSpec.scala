package db

import com.bryzek.dependency.v0.models.{BinarySummary, LibrarySummary, ProjectSummary}
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ItemsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global
/*
  "upsert" in {
    val form = createItemForm()
    val item1 = ItemsDao.create(systemUser, form)

    val item2 = ItemsDao.upsert(systemUser, form)
    item1.guid must be(item2.guid)

    val item3 = createItem()

    item1.guid must be(item2.guid)
    item2.guid must not be(item3.guid)
    item2.label must be(form.label)
  }

  "findByGuid" in {
    val item = createItem()
    ItemsDao.findByGuid(item.guid).map(_.guid) must be(
      Some(item.guid)
    )

    ItemsDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findByObjectGuid" in {
    val binary = createBinary()
    val detail = createItemDetail(binary)
    val form = createItemForm(detail)
    val item = createItem(form)
    ItemsDao.findByObjectGuid(binary.guid).map(_.guid) must be(
      Some(item.guid)
    )

    ItemsDao.findByObjectGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val item1 = createItem()
    val item2 = createItem()

    ItemsDao.findAll(guids = Some(Seq(item1.guid, item2.guid))).map(_.guid).sorted must be(
      Seq(item1.guid, item2.guid).sorted
    )

    ItemsDao.findAll(guids = Some(Nil)) must be(Nil)
    ItemsDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    ItemsDao.findAll(guids = Some(Seq(item1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(item1.guid))
  }

  "supports binaries" in {
    val binary = createBinary()
    val detail = BinarySummary(
      guid = binary.guid,
      name = binary.name
    )
    val itemBinary = createItem(createItemForm(detail))

    val actual = ItemsDao.findByObjectGuid(binary.guid).getOrElse {
      sys.error("Failed to create binary")
    }
    actual.label must be(binary.name.toString)
    actual.detail must be(detail)
  }

  "supports libraries" in {
    val library = createLibrary()
    val detail = LibrarySummary(
      guid = library.guid,
      groupId = library.groupId,
      artifactId = library.artifactId
    )

    val itemLibrary = createItem(createItemForm(detail))
    val actual = ItemsDao.findByObjectGuid(library.guid).getOrElse {
      sys.error("Failed to create library")
    }
    actual.label must be(Seq(library.groupId, library.artifactId).mkString("."))
    actual.detail must be(detail)
  }
 */

  "supports projects" in {
    val project = createProject()
    val detail = ProjectSummary(
      guid = project.guid,
      name = project.name
    )

    val itemProject = createItem(createItemForm(detail))
    val actual = ItemsDao.findByObjectGuid(project.guid).getOrElse {
      sys.error("Failed to create project")
    }
    actual.label must be(project.name)
    actual.detail must be(detail)
  }

}


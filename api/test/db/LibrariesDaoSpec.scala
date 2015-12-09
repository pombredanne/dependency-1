package db

import com.bryzek.dependency.v0.models.SyncEvent
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class LibrariesDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "findByGroupIdAndArtifactId" in {
    val library = createLibrary()
    LibrariesDao.findByGroupIdAndArtifactId(
      library.groupId,
      library.artifactId
    ).map(_.guid) must be(Some(library.guid))

    LibrariesDao.findByGroupIdAndArtifactId(
      library.groupId + "-2",
      library.artifactId
    ) must be (None)

    LibrariesDao.findByGroupIdAndArtifactId(
      library.groupId,
      library.artifactId + "-2"
    ) must be (None)
  }

  "findByGuid" in {
    val library = createLibrary()
    LibrariesDao.findByGuid(library.guid).map(_.guid) must be(
      Some(library.guid)
    )

    LibrariesDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val library1 = createLibrary()
    val library2 = createLibrary()

    LibrariesDao.findAll(guids = Some(Seq(library1.guid, library2.guid))).map(_.guid) must be(
      Seq(library1, library2).sortWith { (x,y) => x.groupId.toString < y.groupId.toString }.map(_.guid)
    )

    LibrariesDao.findAll(guids = Some(Nil)) must be(Nil)
    LibrariesDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    LibrariesDao.findAll(guids = Some(Seq(library1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(library1.guid))
  }

  "findAll by isSynced" in {
    val library = createLibrary()
    createSync(createSyncForm(objectGuid = library.guid, event = SyncEvent.Completed))

    LibrariesDao.findAll(guid = Some(library.guid), isSynced = Some(true)).map(_.guid) must be(Seq(library.guid))
    LibrariesDao.findAll(guid = Some(library.guid), isSynced = Some(false)) must be(Nil)
  }

  "findAll by resolver" in {
    val form = createLibraryForm()
    val library = createLibrary(form)
    val resolver = createResolver()

    LibrariesDao.findAll(resolverGuid = Some(resolver.guid)) must be(Nil)
    LibrariesDao.update(systemUser, library, form.copy(resolverGuid = Some(resolver.guid))).right.get
    LibrariesDao.findAll(resolverGuid = Some(resolver.guid)).map(_.guid) must be(Seq(library.guid))
  }

  "update resolver" in {
    val form = createLibraryForm()
    val library = createLibrary(form)
    val resolver = createResolver()
    val updated = LibrariesDao.update(systemUser, library, form.copy(resolverGuid = Some(resolver.guid))).right.get
    updated.guid must be(library.guid)
    updated.resolver.map(_.guid) must be(Some(resolver.guid))
  }

  "create" must {
    "validates empty group id" in {
      val form = createLibraryForm().copy(groupId = "   ")
      LibrariesDao.validate(form) must be(
        Seq("Group ID cannot be empty")
      )
    }

    "validates empty artifact id" in {
      val form = createLibraryForm().copy(artifactId = "   ")
      LibrariesDao.validate(form) must be(
        Seq("Artifact ID cannot be empty")
      )
    }

    "validates duplicates" in {
      val library = createLibrary()
      val form = createLibraryForm().copy(
        groupId = library.groupId,
        artifactId = library.artifactId
      )
      LibrariesDao.validate(form) must be(
        Seq("Library with this group id and artifact id already exists")
      )
    }
  }

}

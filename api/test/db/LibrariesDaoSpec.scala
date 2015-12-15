package db

import com.bryzek.dependency.v0.models.SyncEvent
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class LibrariesDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "findByGroupIdAndArtifactId" in {
    val library = createLibrary(org)
    LibrariesDao.findByGroupIdAndArtifactId(
      Authorization.All,
      library.groupId,
      library.artifactId
    ).map(_.guid) must be(Some(library.guid))

    LibrariesDao.findByGroupIdAndArtifactId(
      Authorization.All,
      library.groupId + "-2",
      library.artifactId
    ) must be (None)

    LibrariesDao.findByGroupIdAndArtifactId(
      Authorization.All,
      library.groupId,
      library.artifactId + "-2"
    ) must be (None)
  }

  "findByGuid" in {
    val library = createLibrary(org)
    LibrariesDao.findByGuid(Authorization.All, library.guid).map(_.guid) must be(
      Some(library.guid)
    )

    LibrariesDao.findByGuid(Authorization.All, UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val library1 = createLibrary(org)
    val library2 = createLibrary(org)

    LibrariesDao.findAll(Authorization.All, guids = Some(Seq(library1.guid, library2.guid))).map(_.guid) must be(
      Seq(library1, library2).sortWith { (x,y) => x.groupId.toString < y.groupId.toString }.map(_.guid)
    )

    LibrariesDao.findAll(Authorization.All, guids = Some(Nil)) must be(Nil)
    LibrariesDao.findAll(Authorization.All, guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    LibrariesDao.findAll(Authorization.All, guids = Some(Seq(library1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(library1.guid))
  }

  "findAll by isSynced" in {
    val library = createLibrary(org)
    createSync(createSyncForm(objectGuid = library.guid, event = SyncEvent.Completed))

    LibrariesDao.findAll(Authorization.All, guid = Some(library.guid), isSynced = Some(true)).map(_.guid) must be(Seq(library.guid))
    LibrariesDao.findAll(Authorization.All, guid = Some(library.guid), isSynced = Some(false)) must be(Nil)
  }

  "findAll by resolver" in {
    val form = createLibraryForm(org)()
    val library = createLibrary(org)(form)
    val resolver = createResolver()

    LibrariesDao.findAll(Authorization.All, resolverGuid = Some(resolver.guid)) must be(Nil)
    LibrariesDao.update(systemUser, library, form.copy(resolverGuid = Some(resolver.guid))).right.get
    LibrariesDao.findAll(Authorization.All, resolverGuid = Some(resolver.guid)).map(_.guid) must be(Seq(library.guid))
  }

  "update resolver" in {
    val form = createLibraryForm(org)()
    val library = createLibrary(org)(form)
    val resolver = createResolver()
    val updated = LibrariesDao.update(systemUser, library, form.copy(resolverGuid = Some(resolver.guid))).right.get
    updated.guid must be(library.guid)
    updated.resolver.map(_.guid) must be(Some(resolver.guid))
  }

  "create" must {
    "validates empty group id" in {
      val form = createLibraryForm(org)().copy(groupId = "   ")
      LibrariesDao.validate(form) must be(
        Seq("Group ID cannot be empty")
      )
    }

    "validates empty artifact id" in {
      val form = createLibraryForm(org)().copy(artifactId = "   ")
      LibrariesDao.validate(form) must be(
        Seq("Artifact ID cannot be empty")
      )
    }

    "validates duplicates" in {
      val library = createLibrary(org)
      val form = createLibraryForm(org)().copy(
        groupId = library.groupId,
        artifactId = library.artifactId
      )
      LibrariesDao.validate(form) must be(
        Seq("Library with this group id and artifact id already exists")
      )
    }
  }

}

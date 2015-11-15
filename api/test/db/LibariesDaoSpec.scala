package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class LibrariesDaoSpec extends PlaySpec with OneAppPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "findByResolversAndGroupIdAndArtifactId" in {
    val library = Helpers.createLibrary()
    LibrariesDao.findByResolversAndGroupIdAndArtifactId(
      library.resolvers,
      library.groupId,
      library.artifactId
    ).map(_.guid) must be(Some(library.guid))

    LibrariesDao.findByResolversAndGroupIdAndArtifactId(
      library.resolvers ++ Seq("http://other"),
      library.groupId,
      library.artifactId
    ) must be (None)

    LibrariesDao.findByResolversAndGroupIdAndArtifactId(
      library.resolvers,
      library.groupId + "-2",
      library.artifactId
    ) must be (None)

    LibrariesDao.findByResolversAndGroupIdAndArtifactId(
      library.resolvers,
      library.groupId,
      library.artifactId + "-2"
    ) must be (None)
  }

  "findByGuid" in {
    val library = Helpers.createLibrary()
    LibrariesDao.findByGuid(library.guid).map(_.guid) must be(
      Some(library.guid)
    )

    LibrariesDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val library1 = Helpers.createLibrary()
    val library2 = Helpers.createLibrary()

    LibrariesDao.findAll(guids = Some(Seq(library1.guid, library2.guid))).map(_.guid) must be(
      Seq(library1.guid, library2.guid)
    )

    LibrariesDao.findAll(guids = Some(Nil)) must be(Nil)
    LibrariesDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    LibrariesDao.findAll(guids = Some(Seq(library1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(library1.guid))
  }

  "create" must {
    "validates empty group id" in {
      val form = Helpers.createLibraryForm().copy(groupId = "   ")
      LibrariesDao.validate(form).errors.map(_.message) must be(
        Seq("Group ID cannot be empty")
      )
    }

    "validates empty artifact id" in {
      val form = Helpers.createLibraryForm().copy(artifactId = "   ")
      LibrariesDao.validate(form).errors.map(_.message) must be(
        Seq("Artifact ID cannot be empty")
      )
    }

    "validates duplicates" in {
      val library = Helpers.createLibrary()
      val form = Helpers.createLibraryForm().copy(
        resolvers = library.resolvers,
        groupId = library.groupId,
        artifactId = library.artifactId
      )
      LibrariesDao.validate(form).errors.map(_.message) must be(
        Seq("Library with these resolvers, group id and artifact id already exists")
      )
    }
  }

}

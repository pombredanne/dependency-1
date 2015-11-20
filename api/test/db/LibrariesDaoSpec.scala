package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class LibrariesDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "findByResolversAndGroupIdAndArtifactId" in {
    val library = createLibrary()
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
      Seq(library1.guid, library2.guid)
    )

    LibrariesDao.findAll(guids = Some(Nil)) must be(Nil)
    LibrariesDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    LibrariesDao.findAll(guids = Some(Seq(library1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(library1.guid))
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
        resolvers = library.resolvers,
        groupId = library.groupId,
        artifactId = library.artifactId
      )
      LibrariesDao.validate(form) must be(
        Seq("Library with these resolvers, group id and artifact id already exists")
      )
    }
  }

}

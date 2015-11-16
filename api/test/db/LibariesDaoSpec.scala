package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class LibrariesDaoSpec @javax.inject.Inject() (
  helpers: Helpers,
  librariesDao: LibrariesDao
) extends PlaySpec with OneAppPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "findByResolversAndGroupIdAndArtifactId" in {
    val library = helpers.createLibrary()
    librariesDao.findByResolversAndGroupIdAndArtifactId(
      library.resolvers,
      library.groupId,
      library.artifactId
    ).map(_.guid) must be(Some(library.guid))

    librariesDao.findByResolversAndGroupIdAndArtifactId(
      library.resolvers ++ Seq("http://other"),
      library.groupId,
      library.artifactId
    ) must be (None)

    librariesDao.findByResolversAndGroupIdAndArtifactId(
      library.resolvers,
      library.groupId + "-2",
      library.artifactId
    ) must be (None)

    librariesDao.findByResolversAndGroupIdAndArtifactId(
      library.resolvers,
      library.groupId,
      library.artifactId + "-2"
    ) must be (None)
  }

  "findByGuid" in {
    val library = helpers.createLibrary()
    librariesDao.findByGuid(library.guid).map(_.guid) must be(
      Some(library.guid)
    )

    librariesDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val library1 = helpers.createLibrary()
    val library2 = helpers.createLibrary()

    librariesDao.findAll(guids = Some(Seq(library1.guid, library2.guid))).map(_.guid) must be(
      Seq(library1.guid, library2.guid)
    )

    librariesDao.findAll(guids = Some(Nil)) must be(Nil)
    librariesDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    librariesDao.findAll(guids = Some(Seq(library1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(library1.guid))
  }

  "create" must {
    "validates empty group id" in {
      val form = helpers.createLibraryForm().copy(groupId = "   ")
      librariesDao.validate(form).errors.map(_.message) must be(
        Seq("Group ID cannot be empty")
      )
    }

    "validates empty artifact id" in {
      val form = helpers.createLibraryForm().copy(artifactId = "   ")
      librariesDao.validate(form).errors.map(_.message) must be(
        Seq("Artifact ID cannot be empty")
      )
    }

    "validates duplicates" in {
      val library = helpers.createLibrary()
      val form = helpers.createLibraryForm().copy(
        resolvers = library.resolvers,
        groupId = library.groupId,
        artifactId = library.artifactId
      )
      librariesDao.validate(form).errors.map(_.message) must be(
        Seq("Library with these resolvers, group id and artifact id already exists")
      )
    }
  }

}

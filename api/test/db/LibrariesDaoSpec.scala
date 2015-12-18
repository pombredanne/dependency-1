package db

import com.bryzek.dependency.v0.models.{SyncEvent, Visibility}
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

  "findAll by resolver" in {
    val resolver = createResolver(org)
    val form = createLibraryForm(org).copy(resolverGuid = resolver.guid)
    val library = createLibrary(org)(form)

    LibrariesDao.findAll(Authorization.All, resolverGuid = Some(resolver.guid)).map(_.guid) must be(Seq(library.guid))
  }

  "create" must {
    "validates empty group id" in {
      val form = createLibraryForm(org).copy(groupId = "   ")
      LibrariesDao.validate(form) must be(
        Seq("Group ID cannot be empty")
      )
    }

    "validates empty artifact id" in {
      val form = createLibraryForm(org).copy(artifactId = "   ")
      LibrariesDao.validate(form) must be(
        Seq("Artifact ID cannot be empty")
      )
    }

    "validates duplicates" in {
      val library = createLibrary(org)
      val form = createLibraryForm(org).copy(
        groupId = library.groupId,
        artifactId = library.artifactId
      )
      LibrariesDao.validate(form) must be(
        Seq("Library with this group id and artifact id already exists")
      )
    }
  }

  "authorization" must {

    "allow anybody to access a public library" in {
      val user = createUser()
      val org = createOrganization(user = user)
      val resolver = createResolver(org, user) (
        createResolverForm(org).copy(visibility = Visibility.Public)
      )
      val lib = createLibrary(org, user = user)(createLibraryForm(org)(resolver = resolver))

      LibrariesDao.findAll(Authorization.PublicOnly, guid = Some(lib.guid)).map(_.guid) must be(Seq(lib.guid))
      LibrariesDao.findAll(Authorization.All, guid = Some(lib.guid)).map(_.guid) must be(Seq(lib.guid))
      LibrariesDao.findAll(Authorization.Organization(org.guid), guid = Some(lib.guid)).map(_.guid) must be(Seq(lib.guid))
      LibrariesDao.findAll(Authorization.Organization(createOrganization().guid), guid = Some(lib.guid)).map(_.guid) must be(Seq(lib.guid))
      LibrariesDao.findAll(Authorization.User(user.guid), guid = Some(lib.guid)).map(_.guid) must be(Seq(lib.guid))
    }

    "allow only users of an org to access a library w/ a private resolver" in {
      val user = createUser()
      val org = createOrganization(user = user)
      val resolver = createResolver(org, user) (
        createResolverForm(org).copy(visibility = Visibility.Private)
      )
      val lib = createLibrary(org, user = user)(createLibraryForm(org)(resolver = resolver))
      lib.resolver.visibility must be(Visibility.Private)

      LibrariesDao.findAll(Authorization.PublicOnly, guid = Some(lib.guid))must be(Nil)
      LibrariesDao.findAll(Authorization.All, guid = Some(lib.guid)).map(_.guid) must be(Seq(lib.guid))
      LibrariesDao.findAll(Authorization.Organization(org.guid), guid = Some(lib.guid)).map(_.guid) must be(Seq(lib.guid))
      LibrariesDao.findAll(Authorization.Organization(createOrganization().guid), guid = Some(lib.guid))must be(Nil)
      LibrariesDao.findAll(Authorization.User(user.guid), guid = Some(lib.guid)).map(_.guid) must be(Seq(lib.guid))
      LibrariesDao.findAll(Authorization.User(createUser().guid), guid = Some(lib.guid)) must be(Nil)
    }

  }

}

package db

import com.bryzek.dependency.v0.models.{VersionForm, Visibility}
import org.scalatest._
import play.api.db._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class LibraryVersionsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "upsert" in {
    val library = createLibrary(org)()
    val version1 = LibraryVersionsDao.upsert(systemUser, library.guid, VersionForm("1.0", None))
    val version2 = LibraryVersionsDao.upsert(systemUser, library.guid, VersionForm("1.0", None))
    val version3 = LibraryVersionsDao.upsert(systemUser, library.guid, VersionForm("1.1", None))

    version1.guid must be(version2.guid)
    version2.guid must not be(version3.guid)
  }

  "upsert with crossBuildVersion" in {
    val library = createLibrary(org)()
    val version0 = LibraryVersionsDao.upsert(systemUser, library.guid, VersionForm("1.0", None))
    val version1 = LibraryVersionsDao.upsert(systemUser, library.guid, VersionForm("1.0", Some("2.11")))
    val version2 = LibraryVersionsDao.upsert(systemUser, library.guid, VersionForm("1.0", Some("2.11")))
    val version3 = LibraryVersionsDao.upsert(systemUser, library.guid, VersionForm("1.1", Some("2.10")))
    val version4 = LibraryVersionsDao.upsert(systemUser, library.guid, VersionForm("1.1", Some("2.9.3")))

    version0.version must be("1.0")
    version0.crossBuildVersion must be(None)

    version1.version must be("1.0")
    version1.crossBuildVersion must be(Some("2.11"))

    version2.version must be("1.0")
    version2.crossBuildVersion must be(Some("2.11"))

    version3.version must be("1.1")
    version3.crossBuildVersion must be(Some("2.10"))

    version4.version must be("1.1")
    version4.crossBuildVersion must be(Some("2.9.3"))

    version0.guid must not be(version1.guid)
    version1.guid must be(version2.guid)
    version2.guid must not be(version3.guid)
    version3.guid must not be(version4.guid)
  }

  "findByGuid" in {
    val version = createLibraryVersion(org)
    LibraryVersionsDao.findByGuid(Authorization.All, version.guid).map(_.guid) must be(
      Some(version.guid)
    )

    LibraryVersionsDao.findByGuid(Authorization.All, UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val version1 = createLibraryVersion(org)
    val version2 = createLibraryVersion(org)

    LibraryVersionsDao.findAll(Authorization.All, guids = Some(Seq(version1.guid, version2.guid))).map(_.guid).sorted must be(
      Seq(version1.guid, version2.guid).sorted
    )

    LibraryVersionsDao.findAll(Authorization.All, guids = Some(Nil)) must be(Nil)
    LibraryVersionsDao.findAll(Authorization.All, guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    LibraryVersionsDao.findAll(Authorization.All, guids = Some(Seq(version1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(version1.guid))
  }

  "softDelete" in {
    val library = createLibrary(org)()
    val version1 = LibraryVersionsDao.upsert(systemUser, library.guid, VersionForm("1.0", None))
    LibraryVersionsDao.softDelete(systemUser, version1.guid)
    val version2 = LibraryVersionsDao.upsert(systemUser, library.guid, VersionForm("1.0", None))
    val version3 = LibraryVersionsDao.upsert(systemUser, library.guid, VersionForm("1.0", None))

    version1.guid must not be(version2.guid)
    version2.guid must be(version3.guid)
  }

  "authorization" must {

    "allow all to access public libraries" in {
      val user = createUser()
      val org = createOrganization(user = user)
      val resolver = createResolver(org, user)(
        createResolverForm(org = org, visibility = Visibility.Public)
      )
      val library = createLibrary(org, user)(
        createLibraryForm(org, user)(resolver = resolver)
      )
      val libraryVersion = createLibraryVersion(org, user = user)(library = library)
      libraryVersion.library.resolver.visibility must be(Visibility.Public)

      LibraryVersionsDao.findAll(Authorization.PublicOnly, guid = Some(libraryVersion.guid)).map(_.guid) must be(Seq(libraryVersion.guid))
      LibraryVersionsDao.findAll(Authorization.All, guid = Some(libraryVersion.guid)).map(_.guid) must be(Seq(libraryVersion.guid))
      LibraryVersionsDao.findAll(Authorization.Organization(org.guid), guid = Some(libraryVersion.guid)).map(_.guid) must be(Seq(libraryVersion.guid))
      LibraryVersionsDao.findAll(Authorization.Organization(createOrganization().guid), guid = Some(libraryVersion.guid)).map(_.guid) must be(Seq(libraryVersion.guid))
      LibraryVersionsDao.findAll(Authorization.User(user.guid), guid = Some(libraryVersion.guid)).map(_.guid) must be(Seq(libraryVersion.guid))
    }

    "allow only org users to access private libraries" in {
      val user = createUser()
      val org = createOrganization(user = user)
      val resolver = createResolver(org, user)(
        createResolverForm(org = org, visibility = Visibility.Private)
      )
      val library = createLibrary(org, user)(
        createLibraryForm(org, user)(resolver = resolver)
      )
      val libraryVersion = createLibraryVersion(org, user = user)(library = library)
      libraryVersion.library.resolver.visibility must be(Visibility.Private)

      LibraryVersionsDao.findAll(Authorization.All, guid = Some(libraryVersion.guid)).map(_.guid) must be(Seq(libraryVersion.guid))
      LibraryVersionsDao.findAll(Authorization.Organization(org.guid), guid = Some(libraryVersion.guid)).map(_.guid) must be(Seq(libraryVersion.guid))
      LibraryVersionsDao.findAll(Authorization.User(user.guid), guid = Some(libraryVersion.guid)).map(_.guid) must be(Seq(libraryVersion.guid))

      LibraryVersionsDao.findAll(Authorization.PublicOnly, guid = Some(libraryVersion.guid)) must be(Nil)
      LibraryVersionsDao.findAll(Authorization.Organization(createOrganization().guid), guid = Some(libraryVersion.guid)) must be(Nil)
      LibraryVersionsDao.findAll(Authorization.User(createUser().guid), guid = Some(libraryVersion.guid)) must be(Nil)
    }

  }
}

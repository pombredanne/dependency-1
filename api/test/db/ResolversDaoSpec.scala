package db

import com.bryzek.dependency.v0.models.{UsernamePassword, Visibility}
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ResolversDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val org = createOrganization()

  private[this] lazy val publicResolver = ResolversDao.findAll(
    Authorization.All,
    visibility = Some(Visibility.Public),
    limit = 1
  ).headOption.getOrElse {
    sys.error("No public resolvers found")
  }

  "upsert" in {
    val form = createResolverForm(org)
    val resolver1 = ResolversDao.create(systemUser, form).right.get

    val resolver2 = ResolversDao.upsert(systemUser, form).right.get
    resolver1.guid must be(resolver2.guid)

    val resolver3 = createResolver(org)

    resolver2.guid must not be(resolver3.guid)
  }

  "findByGuid" in {
    val resolver = createResolver(org)
    ResolversDao.findByGuid(Authorization.All, resolver.guid).map(_.guid) must be(
      Some(resolver.guid)
    )

    ResolversDao.findByGuid(Authorization.All, UUID.randomUUID) must be(None)
  }

  "findByOrganizationGuidAndUri" in {
    val resolver = createResolver(org)(createResolverForm(org))
    ResolversDao.findByOrganizationGuidAndUri(Authorization.All, org.guid, resolver.uri).map(_.guid) must be(
      Some(resolver.guid)
    )

    ResolversDao.findByOrganizationGuidAndUri(Authorization.All, UUID.randomUUID, resolver.uri).map(_.guid) must be(None)
    ResolversDao.findByOrganizationGuidAndUri(Authorization.All, org.guid, UUID.randomUUID.toString).map(_.guid) must be(None)
  }

  "findAll" must {

    "find by guids" in {
      val resolver1 = createResolver(org)
      val resolver2 = createResolver(org)

      ResolversDao.findAll(Authorization.All, guids = Some(Seq(resolver1.guid, resolver2.guid))).map(_.guid).sorted must be(
        Seq(resolver1.guid, resolver2.guid).sorted
      )

      ResolversDao.findAll(Authorization.All, guids = Some(Nil)) must be(Nil)
      ResolversDao.findAll(Authorization.All, guids = Some(Seq(UUID.randomUUID))) must be(Nil)
      ResolversDao.findAll(Authorization.All, guids = Some(Seq(resolver1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(resolver1.guid))
    }

    "find by organizationGuid" in {
      val resolver = createResolver(org)

      ResolversDao.findAll(Authorization.All, guid = Some(resolver.guid), organizationGuid = Some(org.guid)).map(_.guid).sorted must be(
        Seq(resolver.guid)
      )

      ResolversDao.findAll(Authorization.All, guid = Some(resolver.guid), organizationGuid = Some(createOrganization().guid)) must be(Nil)
    }

    "find by org" in {
      val resolver = createResolver(org)

      ResolversDao.findAll(Authorization.All, guid = Some(resolver.guid), org = Some(org.key)).map(_.guid).sorted must be(
        Seq(resolver.guid)
      )

      ResolversDao.findAll(Authorization.All, guid = Some(resolver.guid), org = Some(createOrganization().key)) must be(Nil)
    }

  }

  "organization" must {

    "be none for public resolvers" in {
      publicResolver.organization must be(None)
    }

    "be set for private resolvers" in {
      val resolver = createResolver(org)(createResolverForm(org = org, visibility = Visibility.Private))
      resolver.organization.map(_.guid) must be(Some(org.guid))
    }

  }

  "private resolvers sort after public" in {
    val resolver = createResolver(org)(createResolverForm(org = org, visibility = Visibility.Private))

    ResolversDao.findAll(
      Authorization.All,
      guids = Some(Seq(publicResolver.guid, resolver.guid))
    ).map(_.guid) must be(Seq(publicResolver.guid, resolver.guid))
  }

  "private resolvers require authorization" in {
    val organization = createOrganization()
    val resolver = createResolver(org)(createResolverForm(
      org = organization,
      visibility = Visibility.Private
    ))

    ResolversDao.findAll(
      Authorization.All,
      guid = Some(resolver.guid)
    ).map(_.guid) must be(Seq(resolver.guid))

    ResolversDao.findAll(
      Authorization.Organization(organization.guid),
      guid = Some(resolver.guid)
    ).map(_.guid) must be(Seq(resolver.guid))

    ResolversDao.findAll(
      Authorization.PublicOnly,
      guid = Some(resolver.guid)
    ) must be(Nil)
  }

  "with username only" in {
    val credentials = UsernamePassword(
      username = "foo"
    )
    val resolver = createResolver(org)(
      createResolverForm(org).copy(
        credentials = Some(
          credentials
        )
      )
    )
    resolver.credentials must be(Some(UsernamePassword(credentials.username, None)))
    ResolversDao.credentials(resolver) must be(Some(credentials))
  }

  "with username and password" in {
    val credentials = UsernamePassword(
      username = "foo",
      password = Some("bar")
    )
    val resolver = createResolver(org)(
      createResolverForm(org).copy(
        credentials = Some(
          credentials
        )
      )
    )
    resolver.credentials must be(Some(UsernamePassword(
      username = credentials.username,
      password = Some("masked")
    )))

    ResolversDao.credentials(resolver) must be(Some(credentials))
  }

  "validates bad URL" in {
    ResolversDao.validate(
      systemUser,
      createResolverForm(org).copy(uri = "foo")
    ) must be(Seq("URI must start with http"))
  }

  "validates duplicate public resolver" in {
    ResolversDao.validate(
      systemUser,
      createResolverForm(org).copy(
        visibility = Visibility.Public,
        uri = publicResolver.uri
      )
    ) must be(Seq(s"Public resolver with uri[${publicResolver.uri}] already exists"))
  }

  "validates duplicate private resolver" in {
    val org = createOrganization()
    val resolver = createResolver(org)(
      createResolverForm(org = org)
    )
    ResolversDao.validate(
      systemUser,
      createResolverForm(org).copy(
        visibility = Visibility.Private,
        organizationGuid = org.guid,
        uri = resolver.uri
      )
    ) must be(Seq(s"Organization already has a resolver with uri[${resolver.uri}]"))
  }

  "validates access to org" in {
    ResolversDao.validate(
      createUser(),
      createResolverForm(org).copy(
        visibility = Visibility.Private
      )
    ) must be(Seq(s"You do not have access to this organization"))
  }

}

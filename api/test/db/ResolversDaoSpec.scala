package db

import com.bryzek.dependency.v0.models.{UsernamePassword, Visibility}
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ResolversDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val publicResolver = ResolversDao.findAll(
    Authorization.All,
    visibility = Some(Visibility.Public),
    limit = 1
  ).headOption.getOrElse {
    sys.error("No public resolvers found")
  }

  "upsert" in {
    val form = createResolverForm()
    val resolver1 = ResolversDao.create(systemUser, form)

    val resolver2 = ResolversDao.upsert(systemUser, form)
    resolver1.guid must be(resolver2.guid)

    val resolver3 = createResolver()

    resolver2.guid must not be(resolver3.guid)
  }

  "findByGuid" in {
    val resolver = createResolver()
    ResolversDao.findByGuid(Authorization.All, resolver.guid).map(_.guid) must be(
      Some(resolver.guid)
    )

    ResolversDao.findByGuid(Authorization.All, UUID.randomUUID) must be(None)
  }

  "findByUserGuidAndUri" in {
    val resolver = createResolver()
    ResolversDao.findByUserGuidAndUri(Authorization.All, resolver.user.guid, resolver.uri).map(_.guid) must be(
      Some(resolver.guid)
    )

    ResolversDao.findByUserGuidAndUri(Authorization.All, UUID.randomUUID, resolver.uri).map(_.guid) must be(None)
    ResolversDao.findByUserGuidAndUri(Authorization.All, resolver.user.guid, UUID.randomUUID.toString).map(_.guid) must be(None)
  }

  "findAll by guids" in {
    val resolver1 = createResolver()
    val resolver2 = createResolver()

    ResolversDao.findAll(Authorization.All, guids = Some(Seq(resolver1.guid, resolver2.guid))).map(_.guid).sorted must be(
      Seq(resolver1.guid, resolver2.guid).sorted
    )

    ResolversDao.findAll(Authorization.All, guids = Some(Nil)) must be(Nil)
    ResolversDao.findAll(Authorization.All, guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    ResolversDao.findAll(Authorization.All, guids = Some(Seq(resolver1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(resolver1.guid))
  }

  "private resolvers sort after public" in {
    val resolver = createResolver(createResolverForm(visibility = Visibility.Private))

    ResolversDao.findAll(
      Authorization.All,
      guids = Some(Seq(publicResolver.guid, resolver.guid))
    ).map(_.guid) must be(Seq(publicResolver.guid, resolver.guid))
  }

  "private resolvers require authorization" in {
    val user = createUser()
    val resolver = createResolver(createResolverForm(
      user = user,
      visibility = Visibility.Private
    ))

    ResolversDao.findAll(
      Authorization.All,
      guid = Some(resolver.guid)
    ).map(_.guid) must be(Seq(resolver.guid))

    ResolversDao.findAll(
      Authorization.User(user.guid),
      guid = Some(resolver.guid)
    ).map(_.guid) must be(Seq(resolver.guid))

    ResolversDao.findAll(
      Authorization.PublicOnly,
      guid = Some(resolver.guid)
    ) must be(Nil)
  }

  "with credentials" in {
    val credentials = UsernamePassword(
      username = "foo",
      password = Some("bar")
    )
    val form = createResolverForm().copy(
      credentials = Some(credentials)
    )
    val resolver = createResolver(form)
    ResolversDao.credentials(resolver) must be(Some(credentials))
  }

}

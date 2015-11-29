package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ResolversDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

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
    ResolversDao.findByGuid(resolver.guid).map(_.guid) must be(
      Some(resolver.guid)
    )

    ResolversDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findByUserGuidAndUri" in {
    val resolver = createResolver()
    ResolversDao.findByUserGuidAndUri(resolver.user.guid, resolver.uri).map(_.guid) must be(
      Some(resolver.guid)
    )

    ResolversDao.findByUserGuidAndUri(UUID.randomUUID, resolver.uri).map(_.guid) must be(None)
    ResolversDao.findByUserGuidAndUri(resolver.user.guid, UUID.randomUUID.toString).map(_.guid) must be(None)
  }

  "findAll by guids" in {
    val resolver1 = createResolver()
    val resolver2 = createResolver()

    ResolversDao.findAll(guids = Some(Seq(resolver1.guid, resolver2.guid))).map(_.guid).sorted must be(
      Seq(resolver1.guid, resolver2.guid).sorted
    )

    ResolversDao.findAll(guids = Some(Nil)) must be(Nil)
    ResolversDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    ResolversDao.findAll(guids = Some(Seq(resolver1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(resolver1.guid))
  }

}

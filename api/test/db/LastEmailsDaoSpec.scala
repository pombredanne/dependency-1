package db

import com.bryzek.dependency.v0.models.Publication
import io.flow.user.v0.models.User
import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._
import java.util.UUID

class LastEmailsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createLastEmail(
    form: LastEmailForm = createLastEmailForm()
  ): LastEmail = {
    LastEmailsDao.record(systemUser, form)
  }

  def createLastEmailForm(
    user: User = createUser()
  ) = LastEmailForm(
    userGuid = user.guid,
    publication = Publication.DailySummary
  )

  "softDelete" in {
    val lastEmail = createLastEmail()
    LastEmailsDao.softDelete(systemUser, lastEmail)
    LastEmailsDao.findByGuid(lastEmail.guid) must be(None)
  }

  "record" in {
    val form = createLastEmailForm()
    val lastEmail1 = createLastEmail(form)
    val lastEmail2 = createLastEmail(form)
    lastEmail1.guid must not be(lastEmail2.guid)

    LastEmailsDao.findByGuid(lastEmail1.guid) must be(None)
    LastEmailsDao.findByGuid(lastEmail2.guid).map(_.guid) must be(Some(lastEmail2.guid))
  }

  "findByUserGuidAndPublication" in {
    val form = createLastEmailForm()
    val lastEmail = createLastEmail(form)

    LastEmailsDao.findByUserGuidAndPublication(form.userGuid, form.publication).map(_.guid) must be(Some(lastEmail.guid))
    LastEmailsDao.findByUserGuidAndPublication(UUID.randomUUID, form.publication).map(_.guid) must be(None)
    LastEmailsDao.findByUserGuidAndPublication(form.userGuid, Publication.UNDEFINED("other")).map(_.guid) must be(None)
  }

}

package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class LanguagesDaoSpec() extends PlaySpec with OneAppPerSuite {

  println("app.injector: " + app.injector)

  val helpers: Helpers = app.injector.instanceOf(classOf[Helpers])
  val languagesDao: LanguagesDao = app.injector.instanceOf(classOf[LanguagesDao])

  import scala.concurrent.ExecutionContext.Implicits.global

  "findByName" in {
    val lang = helpers.createLanguage()
    languagesDao.findByName(lang.name.toString).map(_.name) must be(
      Some(lang.name)
    )

    languagesDao.findByName(UUID.randomUUID.toString) must be(None)
  }

  "findByGuid" in {
    val lang = helpers.createLanguage()
    languagesDao.findByGuid(lang.guid).map(_.guid) must be(
      Some(lang.guid)
    )

    languagesDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val language1 = helpers.createLanguage()
    val language2 = helpers.createLanguage()

    languagesDao.findAll(guids = Some(Seq(language1.guid, language2.guid))).map(_.guid) must be(
      Seq(language1.guid, language2.guid)
    )

    languagesDao.findAll(guids = Some(Nil)) must be(Nil)
    languagesDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    languagesDao.findAll(guids = Some(Seq(language1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(language1.guid))
  }

  "create" must {
    "validates empty name" in {
      val form = helpers.createLanguageForm().copy(name = "   ")
      languagesDao.validate(form).errors.map(_.message) must be(
        Seq("Name cannot be empty")
      )
    }

    "validates duplicate names" in {
      val lang = helpers.createLanguage()
      val form = helpers.createLanguageForm().copy(name = lang.name.toString.toUpperCase)
      languagesDao.validate(form).errors.map(_.message) must be(
        Seq("Language with this name already exists")
      )
    }
  }

}

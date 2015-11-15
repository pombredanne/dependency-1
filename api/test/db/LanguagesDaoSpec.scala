package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class LanguagesDaoSpec extends PlaySpec with OneAppPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "findByName" in {
    val lang = Helpers.createLanguage()
    LanguagesDao.findByName(lang.name.toString).map(_.name) must be(
      Some(lang.name)
    )

    LanguagesDao.findByName(UUID.randomUUID.toString) must be(None)
  }

  "findByGuid" in {
    val lang = Helpers.createLanguage()
    LanguagesDao.findByGuid(lang.guid).map(_.guid) must be(
      Some(lang.guid)
    )

    LanguagesDao.findByGuid(UUID.randomUUID) must be(None)
  }

  "findAll by guids" in {
    val language1 = Helpers.createLanguage()
    val language2 = Helpers.createLanguage()

    LanguagesDao.findAll(guids = Some(Seq(language1.guid, language2.guid))).map(_.guid) must be(
      Seq(language1.guid, language2.guid)
    )

    LanguagesDao.findAll(guids = Some(Nil)) must be(Nil)
    LanguagesDao.findAll(guids = Some(Seq(UUID.randomUUID))) must be(Nil)
    LanguagesDao.findAll(guids = Some(Seq(language1.guid, UUID.randomUUID))).map(_.guid) must be(Seq(language1.guid))
  }

  "create" must {
    "validates empty name" in {
      val form = Helpers.createLanguageForm().copy(name = "   ")
      LanguagesDao.validate(form).errors.map(_.message) must be(
        Seq("Name cannot be empty")
      )
    }

    "validates duplicate names" in {
      val lang = Helpers.createLanguage()
      val form = Helpers.createLanguageForm().copy(name = lang.name.toString.toUpperCase)
      LanguagesDao.validate(form).errors.map(_.message) must be(
        Seq("Language with this name already exists")
      )
    }
  }

}

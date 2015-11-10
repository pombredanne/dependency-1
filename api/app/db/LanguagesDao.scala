package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.{Language, LanguageForm, User}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object LanguagesDao {

  private[this] val BaseQuery = s"""
    select languages.guid,
           languages.name,
           ${AuditsDao.queryCreation("languages")}
      from languages
     where true
  """

  private[this] val InsertQuery = """
    insert into languages
    (guid, name, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {name}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  def validate(
    form: LanguageForm
  ): ValidatedForm[LanguageForm] = {
    val nameErrors = if (form.name.trim == "") {
      Seq("Name cannot be empty")

    } else {
      LanguagesDao.findByName(form.name) match {
        case None => Seq.empty
        case Some(_) => Seq("Language with this name already exists")
      }
    }

    ValidatedForm(form, nameErrors)
  }

  def upsert(createdBy: User, form: LanguageForm): Language = {
    LanguagesDao.findByName(form.name) match {
      case None => {
        create(createdBy, validate(form))
      }
      case Some(lang) => {
        Util.trimmedString(form.version).map { version =>
          // TODO: Create version
        }
        lang
      }
    }
  }

  def create(createdBy: User, valid: ValidatedForm[LanguageForm]): Language = {
    valid.assertValid()

    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'name -> valid.form.name.trim,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    MainActor.ref ! MainActor.Messages.LanguageCreated(guid)

    findByGuid(guid).getOrElse {
      sys.error("Failed to create language")
    }
  }

  def softDelete(deletedBy: User, language: Language) {
    SoftDelete.delete("languages", deletedBy.guid, language.guid)
    MainActor.ref ! MainActor.Messages.LanguageDeleted(language.guid)
  }

  def findByName(name: String): Option[Language] = {
    findAll(name = Some(name), limit = 1).headOption
  }

  def findByGuid(guid: UUID): Option[Language] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    name: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Language] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      projectGuid.map { v => "and languages.guid in (select language_guid from project_languages where project_guid = {project_guid}::uuid and deleted_at is null" },
      name.map { v => "and lower(languages.name) = lower(trim({name}))" },
      isDeleted.map(Filters.isDeleted("languages", _)),
      Some(s"order by languages.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      name.map('name -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Language = {
    Language(
      guid = row[UUID]("guid"),
      name = row[String]("name"),
      audit = AuditsDao.fromRowCreation(row)
    )
  }

}

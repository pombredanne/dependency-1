package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.{Language, LanguageForm}
import io.flow.user.v0.models.User
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
           ${AuditsDao.all("languages")}
      from languages
     where true
  """

  private[this] val InsertQuery = """
    insert into languages
    (guid, name, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {name}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[db] def validate(
    form: LanguageForm
  ): Seq[String] = {
    if (form.name.trim == "") {
      Seq("Name cannot be empty")

    } else {
      LanguagesDao.findByName(form.name) match {
        case None => Seq.empty
        case Some(_) => Seq("Language with this name already exists")
      }
    }
  }

  def upsert(createdBy: User, form: LanguageForm): Either[Seq[String], Language] = {
    LanguagesDao.findByName(form.name) match {
      case None => {
        create(createdBy, form)
      }
      case Some(lang) => {
        DB.withConnection { implicit c =>
          LanguageVersionsDao.upsertWithConnection(createdBy, lang.guid, form.version)
        }
        Right(lang)
      }
    }
  }

  def create(createdBy: User, form: LanguageForm): Either[Seq[String], Language] = {
    validate(form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withTransaction { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'name -> form.name.trim,
            'created_by_guid -> createdBy.guid
          ).execute()

          LanguageVersionsDao.upsertWithConnection(createdBy, guid, form.version)
        }

        MainActor.ref ! MainActor.Messages.LanguageCreated(guid)

        Right(
          findByGuid(guid).getOrElse {
            sys.error("Failed to create language")
          }
        )
      }
      case errors => Left(errors)
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
    guids: Option[Seq[UUID]] = None,
    projectGuid: Option[UUID] = None,
    name: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Language] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and languages.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("languages.guid", _) },
      projectGuid.map { v => """
        and languages.guid in (
          select language_versions.language_guid
            from language_versions
            join project_language_versions
              on project_language_versions.language_version_guid = language_versions.guid
             and project_language_versions.deleted_at is null
             and project_language_versions.project_guid = {project_guid}::uuid
           where language_versions.deleted_at is null
        )
      """.trim },
      name.map { v => "and lower(languages.name) = lower(trim({name}))" },
      isDeleted.map(Filters.isDeleted("languages", _)),
      Some(s"order by lower(languages.name), languages.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      name.map('name -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Language.table("languages").*
      )
    }
  }

}

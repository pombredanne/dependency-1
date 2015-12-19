package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.{Binary, BinaryForm, SyncEvent}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object BinariesDao {

  private[this] val BaseQuery = s"""
    select binaries.guid,
           binaries.name,
           ${AuditsDao.all("binaries")},
           organizations.guid as binaries_organization_guid,
           organizations.key as binaries_organization_key
      from binaries
      left join organizations on organizations.deleted_at is null and organizations.guid = binaries.organization_guid
     where true
  """

  private[this] val InsertQuery = """
    insert into binaries
    (guid, organization_guid, name, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {name}, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[db] def validate(
    form: BinaryForm
  ): Seq[String] = {
    if (form.name.toString.trim == "") {
      Seq("Name cannot be empty")

    } else {
      BinariesDao.findByName(Authorization.All, form.name.toString) match {
        case None => Seq.empty
        case Some(_) => Seq("Binary with this name already exists")
      }
    }
  }

  def upsert(createdBy: User, form: BinaryForm): Either[Seq[String], Binary] = {
    BinariesDao.findByName(Authorization.All, form.name.toString) match {
      case Some(binary) => Right(binary)
      case None => create(createdBy, form)
    }
  }

  def create(createdBy: User, form: BinaryForm): Either[Seq[String], Binary] = {
    validate(form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'organization_guid -> form.organizationGuid,
            'name -> form.name.toString.toLowerCase,
            'created_by_guid -> createdBy.guid
          ).execute()
        }

        MainActor.ref ! MainActor.Messages.BinaryCreated(guid)

        Right(
          findByGuid(Authorization.All, guid).getOrElse {
            sys.error("Failed to create binary")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, binary: Binary) {
    SoftDelete.delete("binaries", deletedBy.guid, binary.guid)
    MainActor.ref ! MainActor.Messages.BinaryDeleted(binary.guid)
  }

  def findByName(auth: Authorization, name: String): Option[Binary] = {
    findAll(auth, name = Some(name), limit = 1).headOption
  }

  def findByGuid(auth: Authorization, guid: UUID): Option[Binary] = {
    findAll(auth, guid = Some(guid), limit = 1).headOption
  }

  /**
    * @param auth: Included here for symmetry with other APIs but at the
    *  moment all binary data are public.
    */
  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    projectGuid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    name: Option[String] = None,
    isSynced: Option[Boolean] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Binary] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and binaries.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("binaries.guid", _) },
      projectGuid.map { v => "and binaries.guid in (select binary_guid from project_binaries where deleted_at is null and binary_guid is not null and project_guid = {project_guid}::uuid" },
      organizationGuid.map { v => "and binaries.organization_guid = {organization_guid}::uuid" },
      name.map { v => "and lower(binaries.name) = lower(trim({name}))" },
      isSynced.map { value =>
        val clause = "select 1 from syncs where object_guid = binaries.guid and event = {sync_event_completed}"
        value match {
          case true => s"and exists ($clause)"
          case false => s"and not exists ($clause)"
        }
      },
      isDeleted.map(Filters.isDeleted("binaries", _)),
      Some(s"order by lower(binaries.name), binaries.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      organizationGuid.map('organization_guid -> _.toString),
      name.map('name -> _.toString),
      isSynced.map(_ => ('sync_event_completed -> SyncEvent.Completed.toString))
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Binary.table("binaries").*
      )
    }
  }

}

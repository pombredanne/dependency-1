package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.{Binary, BinaryForm, SyncEvent}
import io.flow.user.v0.models.User
import io.flow.play.postgresql.{AuditsDao, Query, OrderBy, SoftDelete}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object BinariesDao {

  private[this] val BaseQuery = Query(s"""
    select binaries.guid,
           binaries.name,
           ${AuditsDao.all("binaries")},
           organizations.guid as binaries_organization_guid,
           organizations.key as binaries_organization_key
      from binaries
      left join organizations on organizations.deleted_at is null and organizations.guid = binaries.organization_guid
  """)

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
    orderBy: OrderBy = OrderBy(s"-lower(binaries.name),binaries.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Binary] = {
    DB.withConnection { implicit c =>
      BaseQuery.
        uuid("binaries.guid", guid).
        multi("binaries.guid", guids).
        subquery("binaries.guid", "project_guid", projectGuid, { bindVar =>
          s"select binary_guid from project_binaries where deleted_at is null and binary_guid is not null and project_guid = {$bindVar}::uuid"
        }).
        uuid("binaries.organization_guid", organizationGuid).
        text(
          "binaries.name",
          name,
          columnFunctions = Seq(Query.Function.Lower),
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        condition(
          isSynced.map { value =>
            val clause = "select 1 from syncs where object_guid = binaries.guid and event = {sync_event_completed}"
            value match {
              case true => s"exists ($clause)"
              case false => s"not exists ($clause)"
            }
          }
        ).
        bind("sync_event_completed", isSynced.map(_ => SyncEvent.Completed.toString)).
        nullBoolean("binaries.deleted_at", isDeleted).
        orderBy(orderBy.sql).
        limit(Some(limit)).
        offset(Some(offset)).
        as(
          com.bryzek.dependency.v0.anorm.parsers.Binary.table("binaries").*
        )
    }
  }

}

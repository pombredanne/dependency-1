package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.v0.models.{Binary, BinaryForm, SyncEvent}
import io.flow.user.v0.models.User
import io.flow.postgresql.{Query, OrderBy}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

object BinariesDao {

  private[this] val BaseQuery = Query(s"""
    select binaries.id,
           binaries.name,
           organizations.id as binaries_organization_id,
           organizations.key as binaries_organization_key
      from binaries
      left join organizations on organizations.deleted_at is null and organizations.id = binaries.organization_id
  """)

  private[this] val InsertQuery = """
    insert into binaries
    (id, organization_id, name, updated_by_user_id)
    values
    ({id}, {organization_id}, {name}, {updated_by_user_id})
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
        val id = io.flow.play.util.IdGenerator("bin").randomId()

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'id -> id,
            'organization_id -> form.organizationId,
            'name -> form.name.toString.toLowerCase,
            'updated_by_user_id -> createdBy.id
          ).execute()
        }

        MainActor.ref ! MainActor.Messages.BinaryCreated(id)

        Right(
          findById(Authorization.All, id).getOrElse {
            sys.error("Failed to create binary")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, binary: Binary) {
    SoftDelete.delete("binaries", deletedBy.id, binary.id)
    MainActor.ref ! MainActor.Messages.BinaryDeleted(binary.id)
  }

  def findByName(auth: Authorization, name: String): Option[Binary] = {
    findAll(auth, name = Some(name), limit = 1).headOption
  }

  def findById(auth: Authorization, id: String): Option[Binary] = {
    findAll(auth, id = Some(id), limit = 1).headOption
  }

  /**
    * @param auth: Included here for symmetry with other APIs but at the
    *  moment all binary data are public.
    */
  def findAll(
    auth: Authorization,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    projectId: Option[String] = None,
    organizationId: Option[String] = None,
    name: Option[String] = None,
    isSynced: Option[Boolean] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy(s"-lower(binaries.name),binaries.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Binary] = {
    DB.withConnection { implicit c =>
      BaseQuery.
        equals("binaries.id", id).
        in("binaries.id", ids).
        subquery("binaries.id", "project_id", projectId, { bindVar =>
          s"select binary_id from project_binaries where deleted_at is null and binary_id is not null and project_id = ${bindVar.sql}"
        }).
        equals("binaries.organization_id", organizationId).
        text(
          "binaries.name",
          name,
          columnFunctions = Seq(Query.Function.Lower),
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        condition(
          isSynced.map { value =>
            val clause = "select 1 from syncs where object_id = binaries.id and event = {sync_event_completed}"
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

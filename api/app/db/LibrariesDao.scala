package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.api.lib.Version
import com.bryzek.dependency.v0.models.{Library, LibraryForm}
import io.flow.play.postgresql.{AuditsDao, Filters, SoftDelete}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object LibrariesDao {

  private[this] val BaseQuery = Query(s"""
    select libraries.guid,
           libraries.group_id,
           libraries.artifact_id,
           ${AuditsDao.all("libraries")},
           organizations.guid as libraries_organization_guid,
           organizations.key as libraries_organization_key,
           resolvers.guid as libraries_resolver_guid,
           resolvers.visibility as libraries_resolver_visibility,
           resolvers.uri as libraries_resolver_uri,
           resolver_orgs.guid as libraries_resolver_organization_guid,
           resolver_orgs.key as libraries_resolver_organization_key
      from libraries
      join organizations on organizations.deleted_at is null and organizations.guid = libraries.organization_guid
      join resolvers on resolvers.deleted_at is null and resolvers.guid = libraries.resolver_guid
      left join organizations resolver_orgs on resolver_orgs.deleted_at is null and resolver_orgs.guid = resolvers.organization_guid
  """)

  private[this] val InsertQuery = """
    insert into libraries
    (guid, organization_guid, group_id, artifact_id, resolver_guid, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {group_id}, {artifact_id}, {resolver_guid}::uuid, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[db] def validate(
    form: LibraryForm,
    existing: Option[Library] = None
  ): Seq[String] = {
    val groupIdErrors = if (form.groupId.trim.isEmpty) {
      Seq("Group ID cannot be empty")
    } else {
      Nil
    }

    val artifactIdErrors = if (form.artifactId.trim.isEmpty) {
      Seq("Artifact ID cannot be empty")
    } else {
      Nil
    }

    val existsErrors = if (groupIdErrors.isEmpty && artifactIdErrors.isEmpty) {
      LibrariesDao.findByGroupIdAndArtifactId(Authorization.All, form.groupId, form.artifactId) match {
        case None => Nil
        case Some(lib) => {
          if (Some(lib.guid) == existing.map(_.guid)) {
            Nil
          } else {
            Seq("Library with this group id and artifact id already exists")
          }
        }
      }
    } else {
      Nil
    }

    groupIdErrors ++ artifactIdErrors ++ existsErrors
  }

  def upsert(createdBy: User, form: LibraryForm): Either[Seq[String], Library] = {
    LibrariesDao.findByGroupIdAndArtifactId(Authorization.All, form.groupId, form.artifactId) match {
      case None => {
        create(createdBy, form)
      }
      case Some(lib) => {
        DB.withConnection { implicit c =>
          form.version.foreach { version =>
            LibraryVersionsDao.upsertWithConnection(createdBy, lib.guid, version)
          }
        }
        Right(lib)
      }
    }
  }

  def create(createdBy: User, form: LibraryForm): Either[Seq[String], Library] = {
    validate(form) match {
      case Nil => {
        val guid = UUID.randomUUID

        DB.withTransaction { implicit c =>
          SQL(InsertQuery).on(
            'guid -> guid,
            'organization_guid -> form.organizationGuid,
            'group_id -> form.groupId.trim,
            'artifact_id -> form.artifactId.trim,
            'resolver_guid -> form.resolverGuid,
            'created_by_guid -> createdBy.guid
          ).execute()
          form.version.foreach { version =>
            LibraryVersionsDao.upsertWithConnection(createdBy, guid, version)
          }
        }

        MainActor.ref ! MainActor.Messages.LibraryCreated(guid)

        Right(
          findByGuid(Authorization.All, guid).getOrElse {
            sys.error("Failed to create library")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, library: Library) {
    SoftDelete.delete("libraries", deletedBy.guid, library.guid)
    MainActor.ref ! MainActor.Messages.LibraryDeleted(library.guid)
  }

  def findByGroupIdAndArtifactId(
    auth: Authorization,
    groupId: String,
    artifactId: String
  ): Option[Library] = {
    findAll(
      auth,
      groupId = Some(groupId),
      artifactId = Some(artifactId),
      limit = 1
    ).headOption
  }

  def findByGuid(auth: Authorization, guid: UUID): Option[Library] = {
    findAll(auth, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    organizationGuid: Option[UUID] = None,
    projectGuid: Option[UUID] = None,
    groupId: Option[String] = None,
    artifactId: Option[String] = None,
    resolverGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy.parseOrError("lower(libraries.group_id), lower(libraries.artifact_id), libraries.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Library] = {
    DB.withConnection { implicit c =>    
      Standards.query(
        BaseQuery,
        tableName = "libraries",
        auth = auth.organizations("organizations.guid", Some("resolvers.visibility")),
        guid = guid,
        guids = guids,
        isDeleted = isDeleted,
        orderBy = orderBy,
        limit = limit,
        offset = offset
      ).
        uuid("libraries.organization_guid", organizationGuid).
        subquery("libraries.guid", "project_guid", projectGuid, { bindVar =>
          s"select library_guid from project_libraries where deleted_at is null and project_guid = {$bindVar}::uuid"
        }).
        text(
          "libraries.group_id",
          groupId,
          columnFunctions = Seq(Query.Function.Lower),
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        text(
          "libraries.artifact_id",
          artifactId,
          columnFunctions = Seq(Query.Function.Lower),
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        uuid("libraries.resolver_guid", resolverGuid).
        as(
          com.bryzek.dependency.v0.anorm.parsers.Library.table("libraries").*
        )
    }
  }

}

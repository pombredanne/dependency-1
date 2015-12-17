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

  private[this] val BaseQuery = s"""
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
      left join organizations on organizations.deleted_at is null and organizations.guid = libraries.organization_guid
      left join resolvers on resolvers.deleted_at is null and resolvers.guid = libraries.resolver_guid
      left join organizations resolver_orgs on resolver_orgs.deleted_at is null and resolver_orgs.guid = resolvers.organization_guid
     where true
  """

  private[this] val InsertQuery = """
    insert into libraries
    (guid, organization_guid, group_id, artifact_id, resolver_guid, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {group_id}, {artifact_id}, {resolver_guid}::uuid, {created_by_guid}::uuid, {created_by_guid}::uuid)
  """

  private[this] val UpdateQuery = """
    update libraries
       set group_id = {group_id},
           artifact_id = {artifact_id},
           resolver_guid = {resolver_guid}::uuid,
           updated_by_guid = {updated_by_guid}::uuid
     where guid = {guid}::uuid
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

  def update(updatedBy: User, library: Library, form: LibraryForm): Either[Seq[String], Library] = {
    validate(form, existing = Some(library)) match {
      case Nil => {
        // To support org change - need to record the change as its
        // own record to be able to track changes.
        assert(
          library.organization.guid == form.organizationGuid,
          "Changing organization not currently supported"
        )

        DB.withTransaction { implicit c =>
          SQL(UpdateQuery).on(
            'guid -> library.guid,
            'group_id -> form.groupId.trim,
            'artifact_id -> form.artifactId.trim,
            'resolver_guid -> form.resolverGuid,
            'updated_by_guid -> updatedBy.guid
          ).execute()

          form.version.foreach { version =>
            LibraryVersionsDao.upsertWithConnection(updatedBy, library.guid, version)
          }
        }

        MainActor.ref ! MainActor.Messages.LibraryUpdated(library.guid)

        Right(
          findByGuid(Authorization.All, library.guid).getOrElse {
            sys.error("Failed to update library")
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
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Library] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      Some(auth.organizations("organizations.guid", Some("resolvers.visibility")).and),
      guid.map { v => "and libraries.guid = {guid}::uuid" },
      guids.map { Filters.multipleGuids("libraries.guid", _) },
      organizationGuid.map { v => "and libraries.organization_guid = {organization_guid}::uuid" },
      projectGuid.map { v => """
        and libraries.guid in (
          select library_versions.library_guid
            from library_versions
            join project_library_versions
              on project_library_versions.library_version_guid = library_versions.guid
             and project_library_versions.deleted_at is null
             and project_library_versions.project_guid = {project_guid}::uuid
           where library_versions.deleted_at is null
        )
      """.trim },
      groupId.map { v => "and lower(libraries.group_id) = lower(trim({group_id}))" },
      artifactId.map { v => "and lower(libraries.artifact_id) = lower(trim({artifact_id}))" },
      resolverGuid.map { v => "and libraries.resolver_guid = {resolver_guid}::uuid" },
      isDeleted.map(Filters.isDeleted("libraries", _)),
      Some(s"order by lower(libraries.group_id), lower(libraries.artifact_id), libraries.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      organizationGuid.map('organization_guid -> _.toString),
      projectGuid.map('project_guid -> _.toString),
      groupId.map('group_id -> _.toString),
      artifactId.map('artifact_id -> _.toString),
      resolverGuid.map('resolver_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*).as(
        com.bryzek.dependency.v0.anorm.parsers.Library.table("libraries").*
      )
    }
  }

}

package db

import com.bryzek.dependency.actors.MainActor
import com.bryzek.dependency.api.lib.Version
import com.bryzek.dependency.v0.models.{Library, LibraryForm}
import io.flow.postgresql.{Query, OrderBy}
import io.flow.user.v0.models.User
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

object LibrariesDao {

  private[this] val BaseQuery = Query(s"""
    select libraries.id,
           libraries.group_id,
           libraries.artifact_id,
           organizations.id as libraries_organization_id,
           organizations.key as libraries_organization_key,
           resolvers.id as libraries_resolver_id,
           resolvers.visibility as libraries_resolver_visibility,
           resolvers.uri as libraries_resolver_uri,
           resolver_orgs.id as libraries_resolver_organization_id,
           resolver_orgs.key as libraries_resolver_organization_key
      from libraries
      join organizations on organizations.deleted_at is null and organizations.id = libraries.organization_id
      join resolvers on resolvers.deleted_at is null and resolvers.id = libraries.resolver_id
      left join organizations resolver_orgs on resolver_orgs.deleted_at is null and resolver_orgs.id = resolvers.organization_id
  """)

  private[this] val InsertQuery = """
    insert into libraries
    (id, organization_id, group_id, artifact_id, resolver_id, created_by_id, updated_by_id)
    values
    ({id}, {organization_id}, {group_id}, {artifact_id}, {resolver_id}, {updated_by_user_id})
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
          if (Some(lib.id) == existing.map(_.id)) {
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
            LibraryVersionsDao.upsertWithConnection(createdBy, lib.id, version)
          }
        }
        Right(lib)
      }
    }
  }

  def create(createdBy: User, form: LibraryForm): Either[Seq[String], Library] = {
    validate(form) match {
      case Nil => {
        val id = io.flow.play.util.IdGenerator("lib").randomId()

        DB.withTransaction { implicit c =>
          SQL(InsertQuery).on(
            'id -> id,
            'organization_id -> form.organizationId,
            'group_id -> form.groupId.trim,
            'artifact_id -> form.artifactId.trim,
            'resolver_id -> form.resolverId,
            'updated_by_user_id -> createdBy.id
          ).execute()
          form.version.foreach { version =>
            LibraryVersionsDao.upsertWithConnection(createdBy, id, version)
          }
        }

        MainActor.ref ! MainActor.Messages.LibraryCreated(id)

        Right(
          findById(Authorization.All, id).getOrElse {
            sys.error("Failed to create library")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def softDelete(deletedBy: User, library: Library) {
    SoftDelete.delete("libraries", deletedBy.id, library.id)
    MainActor.ref ! MainActor.Messages.LibraryDeleted(library.id)
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

  def findById(auth: Authorization, id: String): Option[Library] = {
    findAll(auth, id = Some(id), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    organizationId: Option[String] = None,
    projectId: Option[String] = None,
    groupId: Option[String] = None,
    artifactId: Option[String] = None,
    resolverId: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    orderBy: OrderBy = OrderBy("lower(libraries.group_id), lower(libraries.artifact_id), libraries.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Library] = {
    DB.withConnection { implicit c =>    
      Standards.query(
        BaseQuery,
        tableName = "libraries",
        auth = auth.organizations("organizations.id", Some("resolvers.visibility")),
        id = id,
        ids = ids,
        isDeleted = isDeleted,
        orderBy = orderBy.sql,
        limit = Some(limit),
        offset = offset
      ).
        equals("libraries.organization_id", organizationId).
        subquery("libraries.id", "project_id", projectId, { bindVar =>
          s"select library_id from project_libraries where deleted_at is null and project_id = ${bindVar.sql}"
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
        equals("libraries.resolver_id", resolverId).
        as(
          com.bryzek.dependency.v0.anorm.parsers.Library.table("libraries").*
        )
    }
  }

}

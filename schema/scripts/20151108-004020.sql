drop table if exists versions;
drop table if exists artifacts;
drop table if exists projects;

create table projects (
  guid                    uuid primary key,
  scms                    text not null check(enum(scms)),
  name                    text not null check(non_empty_trimmed_string(name))
);

comment on table projects is '
  A project is essentially a source code repository for which we are
  tracking its dependent artifacts.
';

comment on column projects.scms is '
  The source code management system where we find this project.
';

comment on column projects.name is '
  The full name for this project. In github, this will be
  <owner>/<name> (e.g. bryzek/apidoc).
';

select schema_evolution_manager.create_basic_audit_data('public', 'projects');
create unique index projects_scms_lower_name_not_deleted_un_idx on projects(scms, lower(name)) where deleted_at is null;

create table artifacts (
  guid                    uuid primary key,
  group_id                text not null check(non_empty_trimmed_string(group_id)),
  artifact_id             text not null check(non_empty_trimmed_string(artifact_id))
);

comment on table artifacts is '
  Stores all artifacts that we are tracking in some way.
';

select schema_evolution_manager.create_basic_audit_data('public', 'artifacts');
create index on artifacts(group_id);
create index on artifacts(artifact_id);
create unique index artifacts_group_id_artifact_id_not_deleted_un_idx on artifacts(group_id, artifact_id) where deleted_at is null;

create table artifact_versions (
  guid                    uuid primary key,
  artifact_guid           uuid not null references artifacts,
  version                 text not null check(non_empty_trimmed_string(version))
);

comment on table artifact_versions is '
  Stores all artifact_versions of a given artifact - e.g. 9.4-1205-jdbc42
';

select schema_evolution_manager.create_basic_audit_data('public', 'artifact_versions');
create index on artifact_versions(artifact_guid);
create unique index artifact_versions_artifact_guid_version_not_deleted_un_idx on artifact_versions(artifact_guid, version) where deleted_at is null;

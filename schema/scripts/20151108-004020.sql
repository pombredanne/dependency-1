drop table if exists versions;
drop table if exists artifacts;
drop table if exists projects;

create table projects (
  guid                    uuid primary key,
  name                    text not null check(non_empty_trimmed_string(name)),
  scms                    text not null check(enum(scms)),
  uri                     text not null check(non_empty_trimmed_string(uri))
);

comment on table projects is '
  A project is essentially a source code repository for which we are
  tracking its dependent artifacts.
';

select schema_evolution_manager.create_basic_audit_data('public', 'projects');
create unique index projects_lower_name_not_deleted_un_idx on projects(lower(name)) where deleted_at is null;

create table artifacts (
  guid                    uuid primary key,
  group_id                text not null check(non_empty_trimmed_string(group_id)),
  artifact_id             text not null check(non_empty_trimmed_string(group_id))
);

comment on table artifacts is '
  Stores all artifacts that we are tracking in some way.
';

select schema_evolution_manager.create_basic_audit_data('public', 'artifacts');
create index on artifactcs(group_id);
create index on artifactcs(artifact_id);
create unique index artifacts_group_id_artifact_id_not_deleted_un_idx on artifacts(group_id, artifact_id) where deleted_at is null;

create table versions (
  guid                    uuid primary key,
  artifact_guid           uuid not null references artifacts,
  version                 text not null check(non_empty_trimmed_string(version))
);

comment on table versions is '
  Stores all versions of a given artifact - e.g. 9.4-1205-jdbc42
';

select schema_evolution_manager.create_basic_audit_data('public', 'versions');
create index on versions(artifact_guid);
create unique index versions_artifact_guid_version_not_deleted_un_idx on versions(artifact_guid, version) where deleted_at is null;

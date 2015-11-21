drop table if exists project_language_versions;
drop table if exists project_library_versions;

create table project_language_versions (
  guid                    uuid primary key,
  project_guid            uuid not null references projects,
  language_version_guid   uuid not null references language_versions
);

comment on table project_language_versions is '
  Stores the list of language versions that a particular projects depends on
';

select schema_evolution_manager.create_basic_audit_data('public', 'project_language_versions');
create index on project_language_versions(language_version_guid);
create index on project_language_versions(project_guid);
create unique index project_language_versions_language_version_guid_project_guid_not_deleted_un_idx on project_language_versions(language_version_guid, project_guid) where deleted_at is null;

create table project_library_versions (
  guid                    uuid primary key,
  project_guid            uuid not null references projects,
  library_version_guid    uuid not null references library_versions
);

comment on table project_library_versions is '
  Stores the list of library versions that a particular projects depends on
';

select schema_evolution_manager.create_basic_audit_data('public', 'project_library_versions');
create index on project_library_versions(library_version_guid);
create index on project_library_versions(project_guid);
create unique index project_library_versions_library_version_guid_project_guid_not_deleted_un_idx on project_library_versions(library_version_guid, project_guid) where deleted_at is null;

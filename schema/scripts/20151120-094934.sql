drop table if exists project_binary_versions;

create table project_binary_versions (
  guid                    uuid primary key,
  project_guid            uuid not null references projects,
  binary_version_guid   uuid not null references binary_versions
);

comment on table project_binary_versions is '
  Stores the list of binary versions that a particular projects depends on
';

select schema_evolution_manager.create_basic_audit_data('public', 'project_binary_versions');
create index on project_binary_versions(binary_version_guid);
create index on project_binary_versions(project_guid);
create unique index project_binary_versions_binary_version_guid_project_guid_not_deleted_un_idx on project_binary_versions(binary_version_guid, project_guid) where deleted_at is null;

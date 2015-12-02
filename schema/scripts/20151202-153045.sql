drop table if exists project_library_recommendations;
drop table if exists project_binary_recommendations;

create table project_library_recommendations (
  guid                                 uuid primary key,
  project_guid                         uuid not null references projects,
  from_project_library_version_guid    uuid not null references project_library_versions,
  to_library_version_guid              uuid not null references library_versions
);

comment on table project_library_recommendations is '
  For each project we automatically record what our recommendations
  are in terms of which libraries to upgrade. These
  recommendations are created in the background by monitoring updates
  to both the project and its dependencies (for example, if a new
  version of a dependent library is released, we created a
  recommendation). The main use case is to populate the dashboard with
  recommendations for all projects that you are watching.
';

select schema_evolution_manager.create_basic_audit_data('public', 'project_library_recommendations');
create index on project_library_recommendations(project_guid);
create index on project_library_recommendations(from_project_library_version_guid);
create index on project_library_recommendations(to_library_version_guid);

create unique index
 project_library_recommendations_project_guid_from_not_del_un_idx
 on project_library_recommendations(project_guid, from_library_version_guid)
 where deleted_at is null;

create table project_binary_recommendations (
  guid                                 uuid primary key,
  project_guid                         uuid not null references projects,
  from_project_binary_version_guid     uuid not null references project_binary_versions,
  to_binary_version_guid               uuid not null references binary_versions
);

comment on table project_binary_recommendations is '
  For each project we automatically record what our recommendations
  are in terms of which binaries to upgrade. These
  recommendations are created in the background by monitoring updates
  to both the project and its dependencies (for example, if a new
  version of a dependent binary is released, we created a
  recommendation). The main use case is to populate the dashboard with
  recommendations for all projects that you are watching.
';

select schema_evolution_manager.create_basic_audit_data('public', 'project_binary_recommendations');
create index on project_binary_recommendations(project_guid);
create index on project_binary_recommendations(from_binary_version_guid);
create index on project_binary_recommendations(to_binary_version_guid);

create unique index
 project_binary_recommendations_project_guid_from_not_del_un_idx
 on project_binary_recommendations(project_guid, from_binary_version_guid)
 where deleted_at is null;

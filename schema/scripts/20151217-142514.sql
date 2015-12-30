drop table if exists project_libraries;
drop table if exists project_binaries;

create table project_binaries (
  guid                    uuid primary key,
  project_guid            uuid references projects,
  name                    text not null check(util.non_empty_trimmed_string(name)),
  version                 text not null check(util.non_empty_trimmed_string(version)),
  path                    text not null check(util.non_empty_trimmed_string(path)),
  binary_guid             uuid references binaries
);

select audit.setup('public', 'project_binaries');

comment on table project_binaries is '
  Stores all of the binaries that this project depends on.
';

comment on column project_binaries.path is '
  The path relative to the root of the SCMS folder to the
  file in which we found this dependency.
';

create index on project_binaries(project_guid);
create index on project_binaries(binary_guid);
create index on project_binaries(lower(name));
create unique index project_binaries_project_guid_lower_name_version_not_deleted_un_idx on project_binaries(project_guid, lower(name), version) where deleted_at is null;

create table project_libraries (
  guid                    uuid primary key,
  project_guid            uuid references projects,
  group_id                text not null check(util.non_empty_trimmed_string(group_id)),
  artifact_id             text not null check(util.non_empty_trimmed_string(artifact_id)),
  version                 text not null check(util.non_empty_trimmed_string(version)),
  cross_build_version     text check(trim(cross_build_version) = cross_build_version),
  path                    text not null check(util.non_empty_trimmed_string(path)),
  library_guid            uuid references libraries
);

select audit.setup('public', 'project_libraries');

comment on table project_libraries is '
  Stores all of the libraries that this project depends on.
';

comment on column project_libraries.path is '
  The path relative to the root of the SCMS folder to the
  file in which we found this dependency.
';

comment on column project_libraries.library_guid is '
  If we successfully resolve this project library, we associate
  the record with the global library object - serving as the basis
  by which we can make upgrade recommendations.
';

create index on project_libraries(project_guid);
create index on project_libraries(library_guid);
create index on project_libraries(group_id);
create index on project_libraries(artifact_id);
create index on project_libraries(version);

create unique index project_libraries_project_guid_group_id_artifact_id_version_no_cross_not_deleted_un_idx
    on project_libraries(project_guid, group_id, artifact_id, version)
 where deleted_at is null
   and cross_build_version is null;

create unique index project_libraries_project_guid_group_id_artifact_id_version_cross_not_deleted_un_idx
    on project_libraries(project_guid, group_id, artifact_id, version, cross_build_version)
 where deleted_at is null
   and cross_build_version is not null;


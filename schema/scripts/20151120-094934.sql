drop table if exists project_languages;
drop table if exists project_libraries;

create table project_languages (
  guid                    uuid primary key,
  project_guid            uuid not null references projects,
  language_guid           uuid not null references languages
);

comment on table project_languages is '
  Stores the list of languages that a particular projects depends on
';

select schema_evolution_manager.create_basic_audit_data('public', 'project_languages');
create index on project_languages(language_guid);
create index on project_languages(project_guid);
create unique index project_languages_language_guid_project_guid_not_deleted_un_idx on project_languages(language_guid, project_guid) where deleted_at is null;

create table project_libraries (
  guid                    uuid primary key,
  project_guid            uuid not null references projects,
  library_guid           uuid not null references libraries
);

comment on table project_libraries is '
  Stores the list of libraries that a particular projects depends on
';

select schema_evolution_manager.create_basic_audit_data('public', 'project_libraries');
create index on project_libraries(library_guid);
create index on project_libraries(project_guid);
create unique index project_libraries_library_guid_project_guid_not_deleted_un_idx on project_libraries(library_guid, project_guid) where deleted_at is null;

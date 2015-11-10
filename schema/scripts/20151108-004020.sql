drop table if exists language_versions;
drop table if exists languages;
drop table if exists artifact_versions;
drop table if exists artifacts;
drop table if exists projects;
drop table if exists authorizations;
drop table if exists users;

create table users (
  guid                    uuid primary key,
  email                   text not null check(non_empty_trimmed_string(email))
);

comment on table users is '
  Central user database
';

select schema_evolution_manager.create_basic_audit_data('public', 'users');
create unique index users_lower_email_not_deleted_un_idx on users(lower(email)) where deleted_at is null;


create table authorizations (
  guid                    uuid primary key,
  user_guid               uuid not null references users,
  scms                    text not null check(enum(scms)),
  token                   text not null check(non_empty_trimmed_string(token))
);

comment on table authorizations is '
  Stores authorization tokens granted to a user to a specific SCMS
  (e.g. the oauth token to access github).
';

select schema_evolution_manager.create_basic_audit_data('public', 'authorizations');
create index on authorizations(user_guid);
create unique index authorizations_user_guid_scms_token_not_deleted_un_idx
    on authorizations(user_guid, scms, token)
    where deleted_at is null;


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

create table languages (
  guid                    uuid primary key,
  name                    text not null check(enum(name))
);

comment on table languages is '
  Stores all languages that we are tracking in some way (e.g. scala)
';

select schema_evolution_manager.create_basic_audit_data('public', 'languages');
create index on languages(name);
create unique index languages_name_not_deleted_un_idx on languages(name) where deleted_at is null;

create table language_versions (
  guid                    uuid primary key,
  language_guid           uuid not null references languages,
  version                 text not null check(non_empty_trimmed_string(version))
);

comment on table language_versions is '
  Stores all language_versions of a given language - e.g. 2.11.7
';

select schema_evolution_manager.create_basic_audit_data('public', 'language_versions');
create index on language_versions(language_guid);
create unique index language_versions_language_guid_version_not_deleted_un_idx on language_versions(language_guid, version) where deleted_at is null;

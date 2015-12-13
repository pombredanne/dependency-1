drop table if exists resolvers;
drop table if exists binary_versions;
drop table if exists binaries;
drop table if exists library_versions;
drop table if exists libraries;
drop table if exists projects;
drop table if exists authorizations;
drop table if exists users;
drop table if exists organizations;

create table users (
  guid                    uuid primary key,
  email                   text not null check(non_empty_trimmed_string(email)),
  first_name              text check(trim(first_name) = first_name),
  last_name               text check(trim(last_name) = last_name),
  avatar_url              text check(avatar_url is null or trim(avatar_url) = avatar_url)
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

create table organizations (
  guid                    uuid primary key,
  key                     text not null check (enum(key))
);

select schema_evolution_manager.create_basic_audit_data('public', 'organizations');

create unique index organizations_key_not_deleted_un_idx on organizations(key) where deleted_at is null;

comment on table organizations is '
  An organization is the top level entity to which projects,
  libraries, binaries, etc. exist. The primary purpose is to enable
  SAAS - segmenting data by organization.
';

comment on column organizations.key is '
  Used to uniquely identify this organization. URL friendly.
';

create table projects (
  guid                    uuid primary key,
  organization_guid       uuid not null references organizations,
  visibility              text not null check(enum(visibility)),
  scms                    text not null check(enum(scms)),
  name                    text not null check(non_empty_trimmed_string(name)),
  uri                     text not null check(non_empty_trimmed_string(uri))
);

comment on table projects is '
  A project is essentially a source code repository for which we are
  tracking its dependent libraries.
';

comment on column projects.scms is '
  The source code management system where we find this project.
';

comment on column projects.name is '
  The full name for this project. In github, this will be
  <owner>/<name> (e.g. bryzek/apidoc).
';

select schema_evolution_manager.create_basic_audit_data('public', 'projects');
create index on projects(organization_guid);
create unique index projects_organization_scms_lower_name_not_deleted_un_idx on projects(organization_guid, scms, lower(name)) where deleted_at is null;

create table resolvers (
  guid                    uuid primary key,
  visibility              text not null check(enum(visibility)),
  user_guid               uuid not null references users,
  uri                     text not null check(non_empty_trimmed_string(uri)),
  position                integer not null check(position >= 0),
  credentials             json
);

select schema_evolution_manager.create_basic_audit_data('public', 'resolvers');

comment on table resolvers is '
  Stores resolvers we use to find library versions. Resolvers can be
  public or private - and if private follows the user that created the
  resolver.
';

create index on resolvers(user_guid);
create unique index resolvers_user_guid_uri_not_deleted_un_idx on resolvers(user_guid, uri) where deleted_at is null;

create unique index resolvers_public_uri_un_idx on resolvers(uri) where deleted_at is null and visibility = 'public';
create unique index resolvers_public_position_un_idx on resolvers(position) where deleted_at is null and visibility = 'public';

create table libraries (
  guid                    uuid primary key,
  organization_guid       uuid not null references organizations,
  group_id                text not null check(non_empty_trimmed_string(group_id)),
  artifact_id             text not null check(non_empty_trimmed_string(artifact_id)),
  resolver_guid           uuid references resolvers
);

comment on table libraries is '
  Stores all libraries that we are tracking in some way.
';

comment on column libraries.resolver_guid is '
  The resolver we are using to identify versions of this library.
';

select schema_evolution_manager.create_basic_audit_data('public', 'libraries');
create index on libraries(organization_guid);
create index on libraries(group_id);
create index on libraries(artifact_id);
create index on libraries(resolver_guid);
create unique index libraries_organization_group_id_artifact_id_not_deleted_un_idx
    on libraries(organization_guid, group_id, artifact_id)
 where deleted_at is null;

create table library_versions (
  guid                    uuid primary key,
  library_guid            uuid not null references libraries,
  version                 text not null check(non_empty_trimmed_string(version)),
  cross_build_version     text check(trim(cross_build_version) = cross_build_version),
  sort_key                text not null
);

comment on table library_versions is '
  Stores all library_versions of a given library - e.g. 9.4-1205-jdbc42
';

select schema_evolution_manager.create_basic_audit_data('public', 'library_versions');
create index on library_versions(library_guid);

create unique index library_versions_library_guid_lower_version_not_cross_built_not_deleted_un_idx
    on library_versions(library_guid, lower(version))
 where deleted_at is null and cross_build_version is null;

create unique index library_versions_library_guid_lower_version_lower_cross_build_version_not_deleted_un_idx
    on library_versions(library_guid, lower(version), lower(cross_build_version))
 where deleted_at is null and cross_build_version is not null;

create table binaries (
  guid                    uuid primary key,
  organization_guid       uuid not null references organizations,
  name                    text not null check(non_empty_trimmed_string(name))
);

comment on table binaries is '
  Stores all binaries that we are tracking in some way (e.g. scala)
';

select schema_evolution_manager.create_basic_audit_data('public', 'binaries');
create index on binaries(organization_guid);
create unique index binaries_organization_lower_name_not_deleted_un_idx on binaries(organization_guid, lower(name)) where deleted_at is null;

create table binary_versions (
  guid                    uuid primary key,
  binary_guid           uuid not null references binaries,
  version                 text not null check(non_empty_trimmed_string(version)),
  sort_key                text not null
);

comment on table binary_versions is '
  Stores all binary_versions of a given binary - e.g. 2.11.7
';

select schema_evolution_manager.create_basic_audit_data('public', 'binary_versions');
create index on binary_versions(binary_guid);
create unique index binary_versions_binary_guid_version_not_deleted_un_idx on binary_versions(binary_guid, version) where deleted_at is null;


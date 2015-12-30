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
  id                      text primary key,
  email                   text check(util.null_or_non_empty_trimmed_string(email)),
  first_name              text check(trim(first_name) = first_name),
  last_name               text check(trim(last_name) = last_name),
  avatar_url              text check(avatar_url is null or trim(avatar_url) = avatar_url)
);

comment on table users is '
  Central user database
';

select audit.setup('public', 'users');
create unique index users_lower_email_not_deleted_un_idx on users(lower(email)) where deleted_at is null;

create table authorizations (
  id                      text primary key,
  user_id                 text not null references users,
  scms                    text not null check(util.lower_non_empty_trimmed_string(scms)),
  token                   text not null check(util.non_empty_trimmed_string(token))
);

comment on table authorizations is '
  Stores authorization tokens granted to a user to a specific SCMS
  (e.g. the oauth token to access github).
';

select audit.setup('public', 'authorizations');
create index on authorizations(user_id);
create unique index authorizations_user_id_scms_token_not_deleted_un_idx
    on authorizations(user_id, scms, token)
    where deleted_at is null;

create table organizations (
  id                      text primary key,
  key                     text not null check (util.lower_non_empty_trimmed_string(key))
);

select audit.setup('public', 'organizations');

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
  id                      text primary key,
  organization_id         text not null references organizations,
  user_id                 text not null references users,
  visibility              text not null check(util.lower_non_empty_trimmed_string(visibility)),
  scms                    text not null check(util.lower_non_empty_trimmed_string(scms)),
  name                    text not null check(util.non_empty_trimmed_string(name)),
  uri                     text not null check(util.non_empty_trimmed_string(uri))
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

select audit.setup('public', 'projects');
create index on projects(organization_id);
create unique index projects_organization_scms_lower_name_not_deleted_un_idx on projects(organization_id, scms, lower(name)) where deleted_at is null;

create table resolvers (
  id                      text primary key,
  visibility              text not null check(util.lower_non_empty_trimmed_string(visibility)),
  organization_id         text references organizations,
  uri                     text not null check(util.non_empty_trimmed_string(uri)),
  position                integer not null check(position >= 0),
  credentials             json,
  -- can only have credentials if belongs to organization
  constraint resolvers_organization_credentials_ck
       check ( (organization_id is null and credentials is null) or organization_id is not null )
);

select audit.setup('public', 'resolvers');

comment on table resolvers is '
  Stores resolvers we use to find library versions. Resolvers can be
  public or private - and if private follows the organization that
  created the resolver.
';

create index on resolvers(organization_id);
create unique index resolvers_organization_id_uri_not_deleted_un_idx on resolvers(organization_id, uri) where deleted_at is null;

create unique index resolvers_public_uri_un_idx on resolvers(uri) where deleted_at is null and visibility = 'public';
create unique index resolvers_public_position_un_idx on resolvers(position) where deleted_at is null and visibility = 'public';

create table libraries (
  id                      text primary key,
  organization_id         text not null references organizations,
  group_id                text not null check(util.non_empty_trimmed_string(group_id)),
  artifact_id             text not null check(util.non_empty_trimmed_string(artifact_id)),
  resolver_id             text not null references resolvers
);

comment on table libraries is '
  Stores all libraries that we are tracking in some way.
';

comment on column libraries.resolver_id is '
  The resolver we are using to identify versions of this library.
';

select audit.setup('public', 'libraries');
create index on libraries(organization_id);
create index on libraries(group_id);
create index on libraries(artifact_id);
create index on libraries(resolver_id);
create unique index libraries_group_id_artifact_id_not_deleted_un_idx
    on libraries(group_id, artifact_id)
 where deleted_at is null;

create table library_versions (
  id                      text primary key,
  library_id              text not null references libraries,
  version                 text not null check(util.non_empty_trimmed_string(version)),
  cross_build_version     text check(trim(cross_build_version) = cross_build_version),
  sort_key                text not null
);

comment on table library_versions is '
  Stores all library_versions of a given library - e.g. 9.4-1205-jdbc42
';

select audit.setup('public', 'library_versions');
create index on library_versions(library_id);

create unique index library_versions_library_id_lower_version_not_cross_built_not_deleted_un_idx
    on library_versions(library_id, lower(version))
 where deleted_at is null and cross_build_version is null;

create unique index library_versions_library_id_lower_version_lower_cross_build_version_not_deleted_un_idx
    on library_versions(library_id, lower(version), lower(cross_build_version))
 where deleted_at is null and cross_build_version is not null;

create table binaries (
  id                      text primary key,
  organization_id         text not null references organizations,
  name                    text not null check(util.non_empty_trimmed_string(name))
);

comment on table binaries is '
  Stores all binaries that we are tracking in some way (e.g. scala)
';

select audit.setup('public', 'binaries');
create index on binaries(organization_id);
create unique index binaries_lower_name_not_deleted_un_idx on binaries(lower(name)) where deleted_at is null;

create table binary_versions (
  id                      text primary key,
  binary_id               text not null references binaries,
  version                 text not null check(util.non_empty_trimmed_string(version)),
  sort_key                text not null
);

comment on table binary_versions is '
  Stores all binary_versions of a given binary - e.g. 2.11.7
';

select audit.setup('public', 'binary_versions');
create index on binary_versions(binary_id);
create unique index binary_versions_binary_id_version_not_deleted_un_idx on binary_versions(binary_id, version) where deleted_at is null;

